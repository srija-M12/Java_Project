package com.example.studentregistration.service;

import com.example.studentregistration.dto.TimetableEntryResponse;
import com.example.studentregistration.model.Course;
import com.example.studentregistration.model.Registration;
import com.example.studentregistration.model.Student;
import com.example.studentregistration.repository.CourseRepository;
import com.example.studentregistration.repository.RegistrationRepository;
import com.example.studentregistration.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TimetableService {
    private static final int REQUIRED_COURSE_COUNT = 6;
    private static final int MIN_PERIODS_PER_DAY = 4;
    private static final int MAX_PERIODS_PER_DAY = 8;
    private static final List<String> WEEKDAYS = List.of(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"
    );

    private final JdbcTemplate jdbcTemplate;
    private final StudentRepository studentRepository;
    private final RegistrationRepository registrationRepository;
    private final CourseRepository courseRepository;
    private final FacultyAvailabilityService availabilityService;

    public TimetableService(
            JdbcTemplate jdbcTemplate,
            StudentRepository studentRepository,
            RegistrationRepository registrationRepository,
            CourseRepository courseRepository,
            FacultyAvailabilityService availabilityService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.studentRepository = studentRepository;
        this.registrationRepository = registrationRepository;
        this.courseRepository = courseRepository;
        this.availabilityService = availabilityService;
    }

    public List<TimetableEntryResponse> getTimetable(Long studentId) {
        requireStudent(studentId);
        String sql = "SELECT t.timetable_entry_id, t.student_id, t.course_id, c.course_code, "
                + "c.course_name, t.faculty_id, f.faculty_name, t.day_of_week, "
                + "t.start_time, t.end_time FROM timetable_entries t "
                + "JOIN courses c ON c.course_id = t.course_id "
                + "JOIN faculty f ON f.faculty_id = t.faculty_id "
                + "WHERE t.student_id = ? "
                + "ORDER BY FIELD(t.day_of_week,'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'), t.start_time";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new TimetableEntryResponse(
                rs.getLong("timetable_entry_id"), rs.getLong("student_id"),
                rs.getLong("course_id"), rs.getString("course_code"), rs.getString("course_name"),
                rs.getLong("faculty_id"), rs.getString("faculty_name"), rs.getString("day_of_week"),
                rs.getTime("start_time").toLocalTime(), rs.getTime("end_time").toLocalTime()
        ), studentId);
    }

    @Transactional
    public List<TimetableEntryResponse> generate(Long studentId) {
        Student student = requireStudent(studentId);
        List<Registration> registrations = registrationRepository.findByStudentId(studentId);
        List<Long> registeredIds = registrations.stream().map(Registration::getCourseId).toList();
        List<Course> courses = courseRepository.findAllById(registeredIds).stream()
                .filter(course -> course.getDepartment().equalsIgnoreCase(student.getDepartment())
                        && course.getSemester().equals(student.getCurrentSemester())
                        && course.getAcademicYear().equals(student.getAcademicYear()))
                .sorted(Comparator.comparing(Course::getCourseId))
                .toList();
        if (courses.size() != REQUIRED_COURSE_COUNT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Student must have exactly six registrations for their current department, semester, and academic year");
        }

        int weeklyPeriods = courses.stream().mapToInt(course ->
                course.getPeriodsPerWeek() == null ? 0 : course.getPeriodsPerWeek()).sum();
        if (weeklyPeriods < WEEKDAYS.size() * MIN_PERIODS_PER_DAY
                || weeklyPeriods > WEEKDAYS.size() * MAX_PERIODS_PER_DAY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The six selected courses must total 20 to 40 weekly periods to meet the 4-to-8 daily rule");
        }

        List<SavedSlot> savedSlots = jdbcTemplate.query(
                "SELECT t.student_id, t.course_id, t.faculty_id, t.day_of_week, t.start_time, t.end_time "
                        + "FROM timetable_entries t JOIN courses c ON c.course_id = t.course_id "
                        + "WHERE t.student_id <> ? AND c.department = ? AND c.academic_year = ? AND c.semester = ?",
                (rs, rowNum) -> new SavedSlot(
                        rs.getLong("student_id"), rs.getLong("course_id"), rs.getLong("faculty_id"),
                        rs.getString("day_of_week"), rs.getTime("start_time").toLocalTime(),
                        rs.getTime("end_time").toLocalTime()),
                studentId, student.getDepartment(), student.getAcademicYear(), student.getCurrentSemester());

        Map<Long, List<Slot>> facultyOptions = new HashMap<>();
        Map<Long, List<Slot>> sharedCourseSlots = new HashMap<>();
        for (Course course : courses) {
            List<Slot> available = availabilityService.list(course.getFacultyId()).stream()
                    .filter(slot -> Boolean.TRUE.equals(slot.isAvailable()))
                    .filter(slot -> WEEKDAYS.contains(slot.dayOfWeek()))
                    .map(slot -> new Slot(slot.dayOfWeek(), slot.startTime(), slot.endTime()))
                    .distinct()
                    .sorted(slotOrder())
                    .toList();
            if (available.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Faculty " + course.getFacultyId() + " has no available weekday periods for " + course.getCourseCode());
            }
            facultyOptions.put(course.getCourseId(), available);
            List<Slot> shared = savedSlots.stream()
                    .filter(saved -> saved.courseId().equals(course.getCourseId()))
                    .map(SavedSlot::slot)
                    .distinct()
                    .sorted(slotOrder())
                    .toList();
            if (shared.size() > course.getPeriodsPerWeek()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Existing timetable has more meetings than the weekly period count for " + course.getCourseCode());
            }
            if (shared.stream().anyMatch(slot -> !available.contains(slot))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "An existing shared period for " + course.getCourseCode() + " is no longer available to its faculty");
            }
            sharedCourseSlots.put(course.getCourseId(), shared);
        }

        List<ScheduledCourse> scheduled = new ArrayList<>();
        Map<String, Integer> dailyCounts = new HashMap<>();
        WEEKDAYS.forEach(day -> dailyCounts.put(day, 0));

        // Preserve already published meetings for shared courses, then fill remaining weekly periods.
        for (Course course : courses) {
            for (Slot slot : sharedCourseSlots.get(course.getCourseId())) {
                addSlotOrFail(course, slot, scheduled, savedSlots, dailyCounts);
            }
        }

        List<Course> schedulingOrder = new ArrayList<>(courses);
        schedulingOrder.sort(Comparator.comparingInt((Course course) ->
                facultyOptions.get(course.getCourseId()).size() - course.getPeriodsPerWeek()));
        for (Course course : schedulingOrder) {
            Set<Slot> assignedForCourse = new HashSet<>(scheduled.stream()
                    .filter(item -> item.course().getCourseId().equals(course.getCourseId()))
                    .map(ScheduledCourse::slot)
                    .toList());
            int missing = course.getPeriodsPerWeek() - assignedForCourse.size();
            for (int meeting = 0; meeting < missing; meeting++) {
                Slot chosen = facultyOptions.get(course.getCourseId()).stream()
                        .filter(candidate -> !assignedForCourse.contains(candidate))
                        .filter(candidate -> dailyCounts.get(candidate.dayOfWeek()) < MAX_PERIODS_PER_DAY)
                        .filter(candidate -> !hasStudentConflict(candidate, scheduled))
                        .filter(candidate -> !hasFacultyConflict(course.getFacultyId(), course.getCourseId(), candidate, savedSlots))
                        .filter(candidate -> !hasLocalFacultyConflict(course, candidate, scheduled))
                        .min(Comparator
                                .comparingInt((Slot candidate) -> dailyCounts.get(candidate.dayOfWeek()))
                                .thenComparingInt(candidate -> countCoursePeriodsOnDay(course, candidate.dayOfWeek(), scheduled))
                                .thenComparing(slotOrder()))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                                "No conflict-free faculty period is available for " + course.getCourseCode()
                                        + ". Review faculty availability or selected courses."));
                scheduled.add(new ScheduledCourse(course, chosen));
                dailyCounts.compute(chosen.dayOfWeek(), (day, count) -> count + 1);
                assignedForCourse.add(chosen);
            }
        }

        for (String day : WEEKDAYS) {
            int periods = dailyCounts.get(day);
            if (periods < MIN_PERIODS_PER_DAY || periods > MAX_PERIODS_PER_DAY) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Could not build a valid timetable: " + day + " has " + periods
                                + " periods; each weekday must have 4 to 8 periods.");
            }
        }

        jdbcTemplate.update(
                "DELETE t FROM timetable_entries t JOIN courses c ON c.course_id = t.course_id "
                        + "WHERE t.student_id = ? AND c.department = ? AND c.academic_year = ? AND c.semester = ?",
                studentId, student.getDepartment(), student.getAcademicYear(), student.getCurrentSemester());
        jdbcTemplate.batchUpdate(
                "INSERT INTO timetable_entries (student_id, course_id, faculty_id, day_of_week, start_time, end_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                scheduled, scheduled.size(),
                (statement, item) -> {
                    statement.setLong(1, studentId);
                    statement.setLong(2, item.course().getCourseId());
                    statement.setLong(3, item.course().getFacultyId());
                    statement.setString(4, item.slot().dayOfWeek());
                    statement.setTime(5, Time.valueOf(item.slot().startTime()));
                    statement.setTime(6, Time.valueOf(item.slot().endTime()));
                });
        return getTimetable(studentId);
    }

    private void addSlotOrFail(Course course, Slot slot, List<ScheduledCourse> scheduled,
                               List<SavedSlot> savedSlots, Map<String, Integer> dailyCounts) {
        if (hasStudentConflict(slot, scheduled)
                || hasFacultyConflict(course.getFacultyId(), course.getCourseId(), slot, savedSlots)
                || hasLocalFacultyConflict(course, slot, scheduled)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Existing shared periods conflict with the selected courses for " + course.getCourseCode());
        }
        if (dailyCounts.get(slot.dayOfWeek()) >= MAX_PERIODS_PER_DAY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    slot.dayOfWeek() + " exceeds the maximum of eight daily periods");
        }
        scheduled.add(new ScheduledCourse(course, slot));
        dailyCounts.compute(slot.dayOfWeek(), (day, count) -> count + 1);
    }

    private int countCoursePeriodsOnDay(Course course, String day, List<ScheduledCourse> scheduled) {
        return (int) scheduled.stream().filter(item -> item.course().getCourseId().equals(course.getCourseId())
                && item.slot().dayOfWeek().equals(day)).count();
    }

    private Comparator<Slot> slotOrder() {
        return Comparator.comparingInt((Slot slot) -> DayOfWeek.valueOf(slot.dayOfWeek()).getValue())
                .thenComparing(Slot::startTime);
    }

    private Student requireStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    }

    private boolean hasFacultyConflict(Long facultyId, Long courseId, Slot candidate, List<SavedSlot> saved) {
        return saved.stream().anyMatch(slot -> slot.facultyId().equals(facultyId)
                && !slot.courseId().equals(courseId)
                && slot.dayOfWeek().equals(candidate.dayOfWeek())
                && overlaps(candidate.startTime(), candidate.endTime(), slot.startTime(), slot.endTime()));
    }

    private boolean hasLocalFacultyConflict(Course course, Slot candidate, List<ScheduledCourse> scheduled) {
        return scheduled.stream().anyMatch(item -> item.course().getFacultyId().equals(course.getFacultyId())
                && !item.course().getCourseId().equals(course.getCourseId())
                && item.slot().dayOfWeek().equals(candidate.dayOfWeek())
                && overlaps(candidate.startTime(), candidate.endTime(), item.slot().startTime(), item.slot().endTime()));
    }

    private boolean hasStudentConflict(Slot candidate, List<ScheduledCourse> scheduled) {
        return scheduled.stream().anyMatch(item -> item.slot().dayOfWeek().equals(candidate.dayOfWeek())
                && overlaps(candidate.startTime(), candidate.endTime(), item.slot().startTime(), item.slot().endTime()));
    }

    private boolean overlaps(LocalTime firstStart, LocalTime firstEnd, LocalTime secondStart, LocalTime secondEnd) {
        return firstStart.isBefore(secondEnd) && firstEnd.isAfter(secondStart);
    }

    private record Slot(String dayOfWeek, LocalTime startTime, LocalTime endTime) {}
    private record ScheduledCourse(Course course, Slot slot) {}
    private record SavedSlot(Long studentId, Long courseId, Long facultyId, String dayOfWeek,
                             LocalTime startTime, LocalTime endTime) {
        private Slot slot() { return new Slot(dayOfWeek, startTime, endTime); }
    }
}



