import { Fragment, useCallback, useEffect, useState } from 'react'
import './App.css'

const API_URL = import.meta.env.VITE_API_URL || ''
const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']
const LOCAL_TODAY = new Date()

function dateInputValue(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const TODAY_DATE = dateInputValue(LOCAL_TODAY)

function attendanceRange(period) {
  const today = new Date()
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  if (period === 'week') {
    const daysFromMonday = (start.getDay() + 6) % 7
    start.setDate(start.getDate() - daysFromMonday)
    return { from: dateInputValue(start), to: dateInputValue(today) }
  }
  if (period === 'month') {
    const monthStart = new Date(start.getFullYear(), start.getMonth(), 1)
    return { from: dateInputValue(monthStart), to: dateInputValue(today) }
  }
  const todayValue = dateInputValue(start)
  return { from: todayValue, to: todayValue }
}

async function api(path, options = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    credentials: 'include',
    headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers },
  })
  if (!response.ok) {
    let message = `Request failed (${response.status})`
    try { const data = await response.json(); message = data.detail || data.message || data.error || message } catch { /* fallback */ }
    throw new Error(message)
  }
  if (response.status === 204) return null
  return response.json()
}

function App() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [campusCode, setCampusCode] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [roleChoice, setRoleChoice] = useState('STUDENT')
  const [user, setUser] = useState(null)
  const [student, setStudent] = useState(null)
  const [faculty, setFaculty] = useState(null)
  const [availability, setAvailability] = useState([])
  const [facultyTimetable, setFacultyTimetable] = useState([])
  const [courses, setCourses] = useState([])
  const [registered, setRegistered] = useState([])
  const [timetable, setTimetable] = useState([])
  const [attendanceRecords, setAttendanceRecords] = useState([])
  const [attendancePeriod, setAttendancePeriod] = useState('today')
  const [facultyAttendance, setFacultyAttendance] = useState([])
  const [facultyAttendanceDate, setFacultyAttendanceDate] = useState(() => dateInputValue(new Date()))
  const [attendanceDrafts, setAttendanceDrafts] = useState({})
  const [registrationPolicy, setRegistrationPolicy] = useState(null)
  const [selected, setSelected] = useState([])
  const [tab, setTab] = useState('overview')
  const [loading, setLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const loadDashboard = useCallback(async (profile) => {
    setLoading(true); setError('')
    try {
      const term = new URLSearchParams({ department: profile.department, semester: profile.currentSemester, academicYear: profile.academicYear })
      const [catalog, myCourses, schedule, policy] = await Promise.all([api(`/api/courses?${term}`), api(`/api/registrations/${profile.studentId}`), api(`/api/timetables/${profile.studentId}`), api('/api/registrations/policy')])
      setCourses(catalog); setRegistered(myCourses); setTimetable(schedule); setRegistrationPolicy(policy)
    } catch (err) { setError(err.message || 'Could not load your student data.') }
    finally { setLoading(false) }
  }, [])

  const loadFacultyDashboard = useCallback(async (profile) => {
    setLoading(true); setError('')
    try {
      const [slots, classes] = await Promise.all([api(`/api/faculty/${profile.facultyId}/availability`), api('/api/faculty/me/timetable')])
      setAvailability(slots); setFacultyTimetable(classes)
    }
    catch (err) { setError(err.message || 'Could not load your faculty dashboard.') }
    finally { setLoading(false) }
  }, [])

  async function acceptLogin(account) {
    if (account.role !== roleChoice) {
      await api('/api/auth/logout', { method: 'POST' })
      throw new Error(`That account is ${account.role.toLowerCase()}. Choose ${account.role === 'FACULTY' ? 'Faculty' : 'Student'} above and sign in again.`)
    }
    setUser(account)
    setTab(account.role === 'FACULTY' ? 'faculty-dashboard' : 'overview')
    if (account.role === 'FACULTY') {
      try { setFaculty(await api('/api/faculty/me')) }
      catch (err) { setError(err.message || 'Could not find a faculty profile for this account.') }
      return
    }
    if (account.role !== 'STUDENT') throw new Error('Choose a student or faculty account to continue.')
    try { setStudent(await api('/api/students/me')) }
    catch (err) { setError(err.message || 'Could not find a student profile for this account.') }
  }

  async function handleLogin(event) {
    event.preventDefault(); setError(''); setNotice(''); setBusy(true)
    try {
      const account = await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ username: username.trim(), password, campusCode }) })
      setPassword(''); setCampusCode(''); await acceptLogin(account)
    } catch (err) { setError(err.message || 'Could not connect. Check that the backend is running on port 8080.') }
    finally { setBusy(false) }
  }

  async function handleSignup(details) {
    setError(''); setNotice(''); setBusy(true)
    try {
      const account = await api('/api/auth/signup', { method: 'POST', body: JSON.stringify(details) })
      await acceptLogin(account)
      setNotice('Your student account has been created. Welcome to CampusFlow!')
    } catch (err) { setError(err.message || 'Could not create your account.') }
    finally { setBusy(false) }
  }

  async function loadStudentAttendance(period = attendancePeriod) {
    if (!student) return
    setAttendancePeriod(period); setTab('attendance'); setLoading(true); setError('')
    try {
      const range = attendanceRange(period)
      const query = new URLSearchParams(range)
      setAttendanceRecords(await api(`/api/students/me/attendance?${query}`))
    } catch (err) { setError(err.message || 'Could not load your attendance.') }
    finally { setLoading(false) }
  }

  async function loadFacultyAttendance(date = facultyAttendanceDate) {
    if (!faculty || !date) return
    setFacultyAttendanceDate(date); setTab('attendance'); setLoading(true); setError('')
    try {
      const query = new URLSearchParams({ date })
      const entries = await api(`/api/faculty/me/attendance?${query}`)
      setFacultyAttendance(entries)
      setAttendanceDrafts(Object.fromEntries(entries.map((entry) => [entry.timetableEntryId, entry.status || ''])))
    } catch (err) { setError(err.message || 'Could not load the attendance roster.') }
    finally { setLoading(false) }
  }

  async function saveFacultyAttendance(entries) {
    setBusy(true); setError(''); setNotice('')
    try {
      const result = await api('/api/faculty/me/attendance', {
        method: 'POST',
        body: JSON.stringify({ attendanceDate: facultyAttendanceDate, entries: entries.map((entry) => ({
          timetableEntryId: entry.timetableEntryId,
          status: attendanceDrafts[entry.timetableEntryId],
        })) }),
      })
      setNotice(result.message || 'Attendance saved.')
      await loadFacultyAttendance(facultyAttendanceDate)
    } catch (err) { setError(err.message || 'Could not save attendance.') }
    finally { setBusy(false) }
  }

  async function handleLogout() {
    setBusy(true)
    try {
      await api('/api/auth/logout', { method: 'POST' })
      setUser(null); setStudent(null); setFaculty(null); setAvailability([]); setFacultyTimetable([]); setCourses([]); setRegistered([]); setTimetable([]); setAttendanceRecords([]); setFacultyAttendance([]); setAttendanceDrafts({}); setRegistrationPolicy(null); setSelected([]); setError(''); setNotice('')
    } catch (err) { setError(err.message || 'Could not sign out. Please try again.') }
    finally { setBusy(false) }
  }

  useEffect(() => { if (student) loadDashboard(student) }, [student, loadDashboard])
  useEffect(() => { if (faculty) loadFacultyDashboard(faculty) }, [faculty, loadFacultyDashboard])

  function toggleCourse(id) { setSelected((current) => current.includes(id) ? current.filter((courseId) => courseId !== id) : [...current, id]) }
  async function registerCourses() {
    if (!student || selected.length !== 6) return
    setBusy(true); setError(''); setNotice('')
    try { const result = await api('/api/registrations', { method: 'POST', body: JSON.stringify({ studentId: student.studentId, courseIds: selected }) }); setNotice(result.message || 'Your course registration is complete.'); setSelected([]); await loadDashboard(student) }
    catch (err) { setError(err.message || 'Course registration failed.') }
    finally { setBusy(false) }
  }
  async function generateTimetable() {
    if (!student) return
    setBusy(true); setError(''); setNotice('')
    try { setTimetable(await api(`/api/timetables/generate/${student.studentId}`, { method: 'POST' })); setNotice('Your timetable is ready.'); setTab('timetable') }
    catch (err) { setError(err.message || 'Could not generate the timetable.') }
    finally { setBusy(false) }
  }
  async function updateAvailability(slot, isAvailable) {
    setBusy(true); setError(''); setNotice('')
    try {
      const updated = await api(`/api/faculty/availability/${slot.availabilityId}`, { method: 'PUT', body: JSON.stringify({ dayOfWeek: slot.dayOfWeek, startTime: slot.startTime, endTime: slot.endTime, isAvailable }) })
      setAvailability((items) => items.map((item) => item.availabilityId === updated.availabilityId ? updated : item))
      setNotice(`${updated.dayOfWeek} ${updated.startTime.slice(0, 5)} marked ${isAvailable ? 'available' : 'unavailable'}.`)
    } catch (err) { setError(err.message || 'Could not update availability.') }
    finally { setBusy(false) }
  }
  async function addAvailability(slot) {
    setBusy(true); setError(''); setNotice('')
    try {
      const created = await api(`/api/faculty/${faculty.facultyId}/availability`, { method: 'POST', body: JSON.stringify({ ...slot, isAvailable: true }) })
      setAvailability((items) => [...items, created].sort((a, b) => DAYS.indexOf(a.dayOfWeek) - DAYS.indexOf(b.dayOfWeek) || a.startTime.localeCompare(b.startTime)))
      setNotice('Availability slot added.')
    } catch (err) { setError(err.message || 'Could not add this slot.') }
    finally { setBusy(false) }
  }
  async function deleteAvailability(slot) {
    setBusy(true); setError(''); setNotice('')
    try { await api(`/api/faculty/availability/${slot.availabilityId}`, { method: 'DELETE' }); setAvailability((items) => items.filter((item) => item.availabilityId !== slot.availabilityId)); setNotice('Availability slot removed.') }
    catch (err) { setError(err.message || 'Could not remove this slot.') }
    finally { setBusy(false) }
  }

  const firstName = (student?.studentName || faculty?.facultyName || user?.username || 'Student').split(' ')[0]
  const termRegistered = registered.filter((course) => courses.some((item) => item.courseId === course.courseId))
  const hasRegistration = termRegistered.length > 0
  const isFaculty = user?.role === 'FACULTY'
  return (
    <main className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="/" aria-label="CampusFlow"><span className="brand-mark">C<span>F</span></span><span>CampusFlow</span></a>
        <div className="sidebar-label">{isFaculty ? 'FACULTY SPACE' : 'STUDENT SPACE'}</div>
        {isFaculty ? <nav className="side-nav" aria-label="Faculty navigation"><button className={tab === 'faculty-dashboard' ? 'active' : ''} onClick={() => setTab('faculty-dashboard')}><span>▦</span> Dashboard</button><button className={tab === 'attendance' ? 'active' : ''} onClick={() => loadFacultyAttendance()}><span>✓</span> Mark attendance</button><button className={tab === 'availability' ? 'active' : ''} onClick={() => setTab('availability')}><span>◷</span> My availability</button><button className={tab === 'profile' ? 'active' : ''} onClick={() => setTab('profile')}><span>◎</span> My profile</button></nav> : <nav className="side-nav" aria-label="Student navigation"><button className={tab === 'overview' ? 'active' : ''} onClick={() => setTab('overview')}><span>▦</span> Overview</button><button className={tab === 'courses' ? 'active' : ''} onClick={() => setTab('courses')}><span>▤</span> My courses <i>{termRegistered.length || ''}</i></button><button className={tab === 'timetable' ? 'active' : ''} onClick={() => setTab('timetable')}><span>▦</span> Timetable</button><button className={tab === 'attendance' ? 'active' : ''} onClick={() => loadStudentAttendance('today')}><span>✓</span> Check attendance</button><button className={tab === 'profile' ? 'active' : ''} onClick={() => setTab('profile')}><span>◎</span> My profile</button></nav>}
        <div className="sidebar-bottom"><div className="help-box"><span className="help-icon">?</span><b>Need a hand?</b><small>{isFaculty ? 'Update your teaching hours here.' : 'Reach out to your academic office for registration help.'}</small></div><button className="logout-link" onClick={handleLogout} disabled={busy}>↪ <span>Sign out</span></button></div>
      </aside>
      <section className="main-area">
        <header className="dashboard-top"><div className="breadcrumb">{isFaculty ? 'FACULTY PORTAL' : 'STUDENT PORTAL'} <span>/</span> {tab.toUpperCase()}</div><div className="account-chip"><div className="account-avatar">{firstName.slice(0, 1).toUpperCase()}</div><div><b>{faculty?.facultyName || student?.studentName || user?.username || 'Sign in'}</b><small>{faculty?.department || student?.registerNumber || user?.role || 'Academic workspace'}</small></div><span className="account-chevron">⌄</span></div></header>
        <div className="dashboard-content">
          {!user ? <LoginForm username={username} setUsername={setUsername} password={password} setPassword={setPassword} campusCode={campusCode} setCampusCode={setCampusCode} showPassword={showPassword} setShowPassword={setShowPassword} roleChoice={roleChoice} setRoleChoice={setRoleChoice} busy={busy} error={error} notice={notice} onSubmit={handleLogin} onSignup={handleSignup} /> : isFaculty ? !faculty ? <section className="empty-panel"><div className="empty-icon">!</div><h1>Faculty profile unavailable</h1><p>{error || 'Could not load your faculty profile.'}</p><button className="primary-button" onClick={handleLogout}>Sign out and try another account</button></section> : <><div className="welcome-row"><div><div className="date-line">FACULTY WORKSPACE <span className="date-dot" /> {faculty.department}</div><h1>{tab === 'profile' ? 'My profile' : tab === 'availability' ? 'My availability' : tab === 'attendance' ? 'Mark attendance' : 'Faculty dashboard'}</h1><p>{tab === 'profile' ? 'Your faculty account and department details.' : tab === 'availability' ? 'Set the teaching slots that students can use when generating timetables.' : tab === 'attendance' ? 'Record attendance for the classes on your timetable.' : 'See the classes scheduled for you today and throughout the week.'}</p></div><div className="term-badge"><span>FACULTY MEMBER</span><b>{faculty.facultyName}</b><small>{faculty.department} · ID {faculty.facultyId}</small></div></div>{error && <div className="alert error-alert" role="alert">{error}<button onClick={() => setError('')} aria-label="Dismiss">×</button></div>}{notice && <div className="alert success-alert" role="status">✓ {notice}<button onClick={() => setNotice('')} aria-label="Dismiss">×</button></div>}{loading ? <div className="loading-panel"><span className="spinner dark-spinner" /> Loading your faculty dashboard…</div> : tab === 'profile' ? <ProfileView kind="faculty" profile={faculty} account={user} /> : tab === 'availability' ? <FacultyAvailabilityView slots={availability} busy={busy} onUpdate={updateAvailability} onAdd={addAvailability} onDelete={deleteAvailability} /> : tab === 'attendance' ? <FacultyAttendanceView entries={facultyAttendance} date={facultyAttendanceDate} drafts={attendanceDrafts} onDateChange={loadFacultyAttendance} onStatusChange={(id, status) => setAttendanceDrafts((current) => ({ ...current, [id]: status }))} onSave={saveFacultyAttendance} busy={busy} /> : <FacultyDashboardView faculty={faculty} classes={facultyTimetable} onAvailability={() => setTab('availability')} />}</> : !student ? <section className="empty-panel"><div className="empty-icon">!</div><h1>Student profile unavailable</h1><p>{error || 'Could not load your student profile.'}</p><button className="primary-button" onClick={handleLogout}>Sign out and try another account</button></section> : <>
            <div className="welcome-row"><div><div className="date-line">YOUR ACADEMIC WORKSPACE <span className="date-dot" /> {student.academicYear}</div><h1>{tab === 'courses' ? 'Your courses' : tab === 'timetable' ? 'Your timetable' : tab === 'attendance' ? 'Check attendance' : tab === 'profile' ? 'My profile' : <>Good day, {firstName}<span className="wave"> ✳</span></>}</h1><p>{tab === 'overview' ? 'A clear view of your semester starts here.' : tab === 'profile' ? 'Your student and academic details.' : tab === 'attendance' ? 'View the attendance recorded for your scheduled classes.' : `${student.department} · Semester ${student.currentSemester} · ${student.academicYear}`}</p></div><div className="term-badge"><span>CURRENT TERM</span><b>Semester {student.currentSemester}</b><small>{student.department} · {student.academicYear}</small></div></div>
            {error && <div className="alert error-alert" role="alert">{error}<button onClick={() => setError('')} aria-label="Dismiss">×</button></div>}{notice && <div className="alert success-alert" role="status">✓ {notice}<button onClick={() => setNotice('')} aria-label="Dismiss">×</button></div>}
            {loading ? <div className="loading-panel"><span className="spinner dark-spinner" /> Loading your semester information…</div> : tab === 'profile' ? <ProfileView kind="student" profile={student} account={user} /> : tab === 'attendance' ? <StudentAttendanceView period={attendancePeriod} entries={attendanceRecords} onPeriodChange={loadStudentAttendance} /> : tab === 'overview' ? <Overview student={student} registered={termRegistered} timetable={timetable} onCourses={() => setTab('courses')} onTimetable={() => setTab('timetable')} onGenerate={generateTimetable} busy={busy} hasRegistration={hasRegistration} /> : tab === 'courses' ? <CoursesView student={student} policy={registrationPolicy} courses={courses} registered={termRegistered} selected={selected} toggleCourse={toggleCourse} registerCourses={registerCourses} busy={busy} /> : <TimetableView timetable={timetable} onGenerate={generateTimetable} busy={busy} />}
          </>}
        </div>
        {(student || faculty) && <footer className="dashboard-footer">CAMPUSFLOW <span>·</span> COURSE REGISTRATION &amp; TIMETABLE</footer>}
      </section>
    </main>
  )
}

function LoginForm({ username, setUsername, password, setPassword, campusCode, setCampusCode, showPassword, setShowPassword, roleChoice, setRoleChoice, busy, error, notice, onSubmit, onSignup }) {
  const [mode, setMode] = useState('login')
  const [signup, setSignup] = useState({ username: '', email: '', password: '', confirmPassword: '', campusCode: '', studentName: '', registerNumber: '', department: 'CSBS', academicYear: '2026-2027', currentSemester: 3 })
  const [showSignupPassword, setShowSignupPassword] = useState(false)
  const [localError, setLocalError] = useState('')

  function updateSignup(event) {
    const { name, value } = event.target
    setSignup((current) => ({ ...current, [name]: name === 'currentSemester' ? Number(value) : value }))
  }

  async function submitSignup(event) {
    event.preventDefault(); setLocalError('')
    if (signup.password !== signup.confirmPassword) { setLocalError('The passwords do not match.'); return }
    const details = {
      username: signup.username, email: signup.email, password: signup.password,
      campusCode: signup.campusCode,
      studentName: signup.studentName, registerNumber: signup.registerNumber,
      department: signup.department, academicYear: signup.academicYear,
      currentSemester: signup.currentSemester,
    }
    await onSignup(details)
  }

  const activeError = localError || error
  return <div className="login-center"><div className={`login-card ${mode === 'signup' ? 'signup-card' : ''}`}>
    <div className="login-kicker">WELCOME TO CAMPUSFLOW</div>
    <h1>{mode === 'login' ? <>Sign in to your<br /><em>{roleChoice === 'FACULTY' ? 'faculty workspace.' : 'student portal.'}</em></> : <>Create your<br /><em>student account.</em></>}</h1>
    <p className="login-copy">{mode === 'login' ? 'Choose your account type and enter your campus access code.' : 'Add your account and student profile details to get started.'}</p>
    {mode === 'login' && <div className="role-choice" aria-label="Choose account type"><button type="button" className={roleChoice === 'STUDENT' ? 'selected' : ''} onClick={() => setRoleChoice('STUDENT')}>Student</button><button type="button" className={roleChoice === 'FACULTY' ? 'selected' : ''} onClick={() => setRoleChoice('FACULTY')}>Faculty</button></div>}
    {mode === 'login' && <form onSubmit={onSubmit} className="auth-form"><label htmlFor="username">Username</label><input id="username" autoComplete="username" placeholder="Enter your username" value={username} onChange={(event) => setUsername(event.target.value)} required /><label htmlFor="password">Password</label><div className="password-field"><input id="password" type={showPassword ? 'text' : 'password'} autoComplete="current-password" placeholder="Enter your password" value={password} onChange={(event) => setPassword(event.target.value)} required /><button type="button" onClick={() => setShowPassword(!showPassword)}>{showPassword ? 'Hide' : 'Show'}</button></div><label htmlFor="campus-code">Campus code</label><input id="campus-code" inputMode="numeric" autoComplete="off" placeholder="Enter your campus code" value={campusCode} onChange={(event) => setCampusCode(event.target.value)} required /><AuthFeedback error={activeError} notice={notice} /><button className="primary-button full-button" disabled={busy}>{busy ? 'Signing in…' : `Sign in as ${roleChoice === 'FACULTY' ? 'Faculty' : 'Student'}`} <span>↗</span></button></form>}
    {mode === 'signup' && <form onSubmit={submitSignup} className="auth-form signup-form">
      <label htmlFor="signup-name">Student name</label><input id="signup-name" name="studentName" value={signup.studentName} onChange={updateSignup} required maxLength="120" />
      <label htmlFor="signup-register">Register number</label><input id="signup-register" name="registerNumber" value={signup.registerNumber} onChange={updateSignup} required maxLength="30" />
      <div className="signup-two-columns"><div><label htmlFor="signup-department">Department</label><input id="signup-department" name="department" value={signup.department} onChange={updateSignup} required maxLength="80" /></div><div><label htmlFor="signup-semester">Semester</label><select id="signup-semester" name="currentSemester" value={signup.currentSemester} onChange={updateSignup}>{Array.from({ length: 12 }, (_, index) => <option key={index + 1} value={index + 1}>Semester {index + 1}</option>)}</select></div></div>
      <label htmlFor="signup-year">Academic year</label><input id="signup-year" name="academicYear" value={signup.academicYear} onChange={updateSignup} placeholder="2026-2027" required />
      <label htmlFor="signup-email">Email address</label><input id="signup-email" name="email" type="email" autoComplete="email" value={signup.email} onChange={updateSignup} required maxLength="254" />
      <label htmlFor="signup-username">Username</label><input id="signup-username" name="username" autoComplete="username" value={signup.username} onChange={updateSignup} required minLength="3" maxLength="50" />
      <label htmlFor="signup-password">Password</label><div className="password-field"><input id="signup-password" name="password" type={showSignupPassword ? 'text' : 'password'} autoComplete="new-password" value={signup.password} onChange={updateSignup} required minLength="8" maxLength="72" /><button type="button" onClick={() => setShowSignupPassword(!showSignupPassword)}>{showSignupPassword ? 'Hide' : 'Show'}</button></div>
      <label htmlFor="signup-confirm-password">Confirm password</label><input id="signup-confirm-password" name="confirmPassword" type="password" autoComplete="new-password" value={signup.confirmPassword} onChange={updateSignup} required minLength="8" maxLength="72" />
      <label htmlFor="signup-campus-code">Campus code</label><input id="signup-campus-code" name="campusCode" inputMode="numeric" autoComplete="off" value={signup.campusCode} onChange={updateSignup} required />
      <AuthFeedback error={activeError} notice={notice} /><button className="primary-button full-button" disabled={busy}>{busy ? 'Creating account…' : 'Create student account ↗'}</button>
    </form>}
    {mode === 'login' && <div className="signup-invite">{roleChoice === 'STUDENT' ? <>Don’t have a student account? <button type="button" onClick={() => { setMode('signup'); setLocalError('') }}>Sign up</button></> : 'Faculty accounts are created by your institution.'}</div>}
    {mode !== 'login' && <button type="button" className="auth-back-link" onClick={() => { setMode('login'); setLocalError('') }}>← Back to sign in</button>}
    <div className="login-note">◈ Secure access to your academic workspace</div>
  </div></div>
}

function FacultyAttendanceView({ entries, date, drafts, onDateChange, onStatusChange, onSave, busy }) {
  const grouped = Object.values(entries.reduce((groups, entry) => {
    const key = `${entry.courseId}-${entry.dayOfWeek}-${entry.startTime}`
    if (!groups[key]) groups[key] = { ...entry, students: [] }
    groups[key].students.push(entry)
    return groups
  }, {}))
  const today = TODAY_DATE

  return <section className="attendance-page">
    <div className="attendance-toolbar"><div><span className="card-eyebrow">FACULTY ROSTER</span><h2>Classes for this date</h2><p>Choose Present or Absent for each student, then save that class.</p></div><label>Date<input type="date" value={date} max={today} onChange={(event) => onDateChange(event.target.value)} /></label></div>
    {grouped.length === 0 ? <div className="attendance-empty"><span>◷</span><h3>No classes on this date</h3><p>There are no timetable periods assigned to you for {new Intl.DateTimeFormat('en-IN', { dateStyle: 'full' }).format(Date.parse(`${date}T00:00:00`))}.</p></div> : <div className="attendance-class-list">{grouped.map((group) => {
      const complete = group.students.every((student) => ['PRESENT', 'ABSENT'].includes(drafts[student.timetableEntryId]))
      return <article className="content-card attendance-class-card" key={`${group.courseId}-${group.startTime}`}>
        <div className="card-heading"><div><span className="card-eyebrow">{group.courseCode} · {group.startTime.slice(0, 5)}–{group.endTime.slice(0, 5)}</span><h2>{group.courseName}</h2></div><span className="count-pill">{group.students.length} students</span></div>
        <div className="attendance-roster">{group.students.map((student) => <div className="attendance-roster-row" key={student.timetableEntryId}><div><b>{student.studentName}</b><small>{student.registerNumber}</small></div><select aria-label={`Attendance for ${student.studentName}`} value={drafts[student.timetableEntryId] || ''} onChange={(event) => onStatusChange(student.timetableEntryId, event.target.value)}><option value="">Choose status</option><option value="PRESENT">Present</option><option value="ABSENT">Absent</option></select></div>)}</div>
        <button className="primary-button attendance-save-button" disabled={busy || !complete} onClick={() => onSave(group.students)}>{busy ? 'Saving…' : 'Save attendance'}</button>
      </article>
    })}</div>}
  </section>
}

function StudentAttendanceView({ period, entries, onPeriodChange }) {
  const present = entries.filter((entry) => entry.status === 'PRESENT').length
  const absent = entries.filter((entry) => entry.status === 'ABSENT').length
  const notMarked = entries.filter((entry) => entry.status === 'NOT_MARKED').length
  const grouped = entries.reduce((result, entry) => {
    (result[entry.attendanceDate] ||= []).push(entry)
    return result
  }, {})
  const sections = Object.entries(grouped).sort(([a], [b]) => a.localeCompare(b))

  return <section className="attendance-page">
    <div className="attendance-period-tabs" role="tablist" aria-label="Attendance date range">{[['today', 'Today'], ['week', 'This week'], ['month', 'This month']].map(([value, label]) => <button type="button" role="tab" aria-selected={period === value} className={period === value ? 'selected' : ''} key={value} onClick={() => onPeriodChange(value)}>{label}</button>)}</div>
    <div className="attendance-summary"><div><small>PRESENT</small><b>{present}</b></div><div><small>ABSENT</small><b>{absent}</b></div><div><small>NOT MARKED</small><b>{notMarked}</b></div></div>
    {sections.length === 0 ? <div className="attendance-empty"><span>✓</span><h3>No timetable periods found</h3><p>Your scheduled periods will appear here after a timetable has been generated.</p></div> : <div className="attendance-day-list">{sections.map(([date, dayEntries]) => <article className="content-card attendance-day-card" key={date}>
      <div className="card-heading"><div><span className="card-eyebrow">{date === TODAY_DATE ? 'TODAY' : dayEntries[0].dayOfWeek}</span><h2>{new Intl.DateTimeFormat('en-IN', { weekday: 'long', day: 'numeric', month: 'long' }).format(Date.parse(`${date}T00:00:00`))}</h2></div><span className="count-pill">{dayEntries.length} periods</span></div>
      <div className="attendance-student-list">{dayEntries.map((entry) => <div className="attendance-student-row" key={`${date}-${entry.timetableEntryId}`}><div className="attendance-time">{entry.startTime.slice(0, 5)}<small>{entry.endTime.slice(0, 5)}</small></div><div className="attendance-subject"><b>{entry.courseCode} · {entry.courseName}</b><small>{entry.facultyName}</small></div><span className={`attendance-status ${entry.status.toLowerCase().replace('_', '-')}`}>{entry.status === 'NOT_MARKED' ? 'Not marked' : entry.status === 'PRESENT' ? 'Present' : 'Absent'}</span></div>)}</div>
    </article>)}</div>}
  </section>
}

function AuthFeedback({ error, notice }) {
  return <>{error && <p className="form-error" role="alert">{error}</p>}{notice && <p className="form-notice" role="status">{notice}</p>}</>
}

function FacultyDashboardView({ faculty, classes, onAvailability }) {
  const today = new Intl.DateTimeFormat('en-US', { weekday: 'long' }).format(LOCAL_TODAY).toUpperCase()
  const todayClasses = classes.filter((item) => item.dayOfWeek === today)
  const activeDays = DAYS.filter((day) => classes.some((item) => item.dayOfWeek === day))
  const nextTeachingDay = DAYS.find((day) => DAYS.indexOf(day) > DAYS.indexOf(today)) || DAYS[0]
  const nextDayClasses = classes.filter((item) => item.dayOfWeek === nextTeachingDay)
  const todayLabel = new Intl.DateTimeFormat('en-IN', { weekday: 'long', day: 'numeric', month: 'long' }).format(LOCAL_TODAY)

  return <div className="faculty-dashboard">
    <div className="faculty-metric-grid">
      <div className="faculty-metric-card"><span className="faculty-metric-icon">◷</span><small>CLASSES TODAY</small><b>{todayClasses.length}</b><span>{todayLabel}</span></div>
      <div className="faculty-metric-card"><span className="faculty-metric-icon">▦</span><small>THIS WEEK</small><b>{classes.length}</b><span>scheduled class meetings</span></div>
      <div className="faculty-metric-card"><span className="faculty-metric-icon">◎</span><small>TEACHING DAYS</small><b>{activeDays.length}</b><span>days with scheduled classes</span></div>
    </div>
    <div className="faculty-today-layout">
      <section className="content-card faculty-today-card">
        <div className="card-heading"><div><span className="card-eyebrow">TODAY'S PLAN</span><h2>{today} classes</h2><p className="card-subtitle">Your teaching schedule for {todayLabel}.</p></div><span className="count-pill">{todayClasses.length} {todayClasses.length === 1 ? 'class' : 'classes'}</span></div>
        {todayClasses.length ? <div className="faculty-class-list">{todayClasses.map((item) => <FacultyClassCard item={item} key={`${item.courseId}-${item.startTime}`} />)}</div> : <div className="faculty-no-classes"><span>✓</span><div><b>No classes scheduled today</b><p>{nextDayClasses.length ? `Your next teaching day is ${nextTeachingDay[0] + nextTeachingDay.slice(1).toLowerCase()} with ${nextDayClasses.length} ${nextDayClasses.length === 1 ? 'class' : 'classes'}.` : 'Your weekly classes will appear here after a timetable is generated.'}</p></div></div>}
      </section>
      <aside className="content-card faculty-dashboard-tip"><span className="card-eyebrow">YOUR TEACHING</span><h2>{faculty.facultyName}</h2><p>{faculty.department} · Faculty ID {faculty.facultyId}</p><div className="faculty-course-count"><b>{new Set(classes.map((item) => item.courseId)).size}</b><span>courses on your timetable</span></div><button className="secondary-button" onClick={onAvailability}>Manage availability ↗</button></aside>
    </div>
    <section className="content-card faculty-week-card"><div className="card-heading"><div><span className="card-eyebrow">WEEK AT A GLANCE</span><h2>Your weekly classes</h2><p className="card-subtitle">Classes are grouped by weekday and time.</p></div></div>
      {classes.length ? <div className="faculty-week-grid">{DAYS.map((day) => { const dayClasses = classes.filter((item) => item.dayOfWeek === day); return <section className={`faculty-weekday ${day === today ? 'faculty-weekday-today' : ''}`} key={day}><div className="faculty-weekday-heading"><b>{day}</b><span>{dayClasses.length} {dayClasses.length === 1 ? 'class' : 'classes'}</span></div>{dayClasses.length ? <div className="faculty-weekday-classes">{dayClasses.map((item) => <FacultyClassCard item={item} key={`${item.courseId}-${item.startTime}`} compact />)}</div> : <p className="faculty-day-empty">No classes</p>}</section> })}</div> : <div className="faculty-week-empty"><b>No timetable entries yet</b><p>When students generate timetables for your courses, your scheduled classes will appear here.</p></div>}
    </section>
  </div>
}

function FacultyClassCard({ item, compact = false }) {
  return <article className={`faculty-class-card ${compact ? 'faculty-class-card-compact' : ''}`}>
    <div className="faculty-class-time">{formatClock(item.startTime)}<span>–</span>{formatClock(item.endTime)}</div>
    <div className="faculty-class-details"><b>{item.courseCode} <span>· Sem {item.semester}</span></b><p>{item.courseName}</p><small>{item.academicYear}</small></div>
    <span className="faculty-student-count">{item.studentCount} {item.studentCount === 1 ? 'student' : 'students'}</span>
  </article>
}

function FacultyAvailabilityView({ slots, busy, onUpdate, onAdd, onDelete }) {
  const [day, setDay] = useState('MONDAY')
  const [startTime, setStartTime] = useState('09:00')
  const [endTime, setEndTime] = useState('10:00')
  const [formError, setFormError] = useState('')
  async function submitSlot(event) {
    event.preventDefault(); setFormError('')
    if (endTime <= startTime) { setFormError('End time must be later than start time.'); return }
    await onAdd({ dayOfWeek: day, startTime: `${startTime}:00`, endTime: `${endTime}:00` })
  }
  const ordered = [...slots].sort((a, b) => DAYS.indexOf(a.dayOfWeek) - DAYS.indexOf(b.dayOfWeek) || a.startTime.localeCompare(b.startTime))
  return <div className="faculty-layout"><section className="content-card full-card"><div className="card-heading"><div><span className="card-eyebrow">TEACHING SCHEDULE</span><h2>Availability slots</h2><p className="card-subtitle">Choose when you are available to teach. Students’ timetable generation uses available slots.</p></div><span className="count-pill">{slots.filter((slot) => slot.isAvailable).length} available</span></div>{ordered.length ? <div className="availability-list">{DAYS.map((weekday) => { const daySlots = ordered.filter((slot) => slot.dayOfWeek === weekday); return daySlots.length ? <div className="availability-day" key={weekday}><h3>{weekday}</h3><div className="availability-day-slots">{daySlots.map((slot) => <div className={`availability-slot ${slot.isAvailable ? 'slot-open' : 'slot-closed'}`} key={slot.availabilityId}><span className="slot-time">{slot.startTime.slice(0, 5)} – {slot.endTime.slice(0, 5)}</span><span className="slot-status">{slot.isAvailable ? 'Available' : 'Unavailable'}</span><button type="button" className="slot-toggle" disabled={busy} onClick={() => onUpdate(slot, !slot.isAvailable)}>{slot.isAvailable ? 'Mark unavailable' : 'Mark available'}</button><button type="button" className="slot-delete" disabled={busy} onClick={() => onDelete(slot)} aria-label={`Remove ${weekday} ${slot.startTime}`}>×</button></div>)}</div></div> : null })}</div> : <p className="empty-message">No availability slots have been added yet.</p>}</section><section className="content-card add-slot-card"><span className="card-eyebrow">ADD A TEACHING TIME</span><h2>Add availability</h2><p className="card-subtitle">Add another slot students can use.</p><form className="slot-form" onSubmit={submitSlot}><label htmlFor="slot-day">Day</label><select id="slot-day" value={day} onChange={(event) => setDay(event.target.value)}>{DAYS.map((weekday) => <option key={weekday} value={weekday}>{weekday[0] + weekday.slice(1).toLowerCase()}</option>)}</select><div className="time-inputs"><div><label htmlFor="slot-start">Starts</label><input id="slot-start" type="time" value={startTime} onChange={(event) => setStartTime(event.target.value)} required /></div><div><label htmlFor="slot-end">Ends</label><input id="slot-end" type="time" value={endTime} onChange={(event) => setEndTime(event.target.value)} required /></div></div>{formError && <p className="form-error">{formError}</p>}<button className="primary-button" disabled={busy}>Add slot +</button></form></section></div>
}

function ProfileView({ kind, profile, account }) {
  const isFaculty = kind === 'faculty'
  const name = isFaculty ? profile.facultyName : profile.studentName
  const details = isFaculty ? [
    ['Faculty name', profile.facultyName], ['Faculty ID', profile.facultyId], ['Department', profile.department],
    ['Username', account.username], ['Email address', account.email], ['Account type', 'Faculty'],
  ] : [
    ['Student name', profile.studentName], ['Register number', profile.registerNumber], ['Department', profile.department],
    ['Current semester', `Semester ${profile.currentSemester}`], ['Academic year', profile.academicYear],
    ['Username', account.username], ['Email address', account.email], ['Account type', 'Student'],
  ]
  return <section className="profile-layout"><div className="content-card profile-identity"><div className="profile-avatar">{name?.slice(0, 1).toUpperCase() || 'U'}</div><div className="card-eyebrow">{isFaculty ? 'FACULTY PROFILE' : 'STUDENT PROFILE'}</div><h2>{name}</h2><p>{account.email}</p><span className="profile-role">{isFaculty ? 'Faculty member' : 'Student'} · {profile.department}</span></div><div className="content-card profile-details"><div className="card-heading"><div><span className="card-eyebrow">ACCOUNT INFORMATION</span><h2>Personal details</h2><p className="card-subtitle">Details linked to your CampusFlow account.</p></div><span className="profile-lock">▣ Secure</span></div><div className="profile-detail-grid">{details.map(([label, value]) => <div className="profile-detail" key={label}><span>{label}</span><b>{value || '—'}</b></div>)}</div><div className="profile-readonly-note">Profile details are read from your account and academic records.</div></div></section>
}
function Overview({ student, registered, timetable, onCourses, onTimetable, onGenerate, busy, hasRegistration }) {
  return <><div className="stats-grid"><StatCard icon="▤" label="REGISTERED COURSES" value={`${registered.length}`.padStart(2, '0')} note={hasRegistration ? 'Courses in your current term' : 'No courses registered yet'} /><StatCard icon="◷" label="SCHEDULED CLASSES" value={`${timetable.length}`.padStart(2, '0')} note={timetable.length ? 'Classes in your timetable' : 'Timetable not generated'} /><StatCard icon="◎" label="DEPARTMENT" value={student.department} note={`Semester ${student.currentSemester}`} /></div><div className="content-grid"><section className="content-card course-summary"><div className="card-heading"><div><span className="card-eyebrow">YOUR LEARNING</span><h2>Course registration</h2></div><button className="text-button" onClick={onCourses}>View courses ↗</button></div>{hasRegistration ? <><div className="course-list-compact">{registered.slice(0, 4).map((course) => <div className="compact-course" key={course.courseId}><span className="course-symbol">{course.courseCode?.slice(0, 2)}</span><span className="compact-title"><b>{course.courseName}</b><small>{course.courseCode} · {course.credits} credits</small></span><span className="registered-check">✓</span></div>)}</div><button className="secondary-button" onClick={onCourses}>See all {registered.length} registered courses</button></> : <div className="empty-inline"><p>Choose six courses from your current semester to complete registration.</p><button className="primary-button" onClick={onCourses}>Browse available courses ↗</button></div>}</section><section className="content-card schedule-summary"><div className="card-heading"><div><span className="card-eyebrow">PLAN YOUR WEEK</span><h2>Timetable</h2></div><button className="text-button" onClick={onTimetable}>Open timetable ↗</button></div>{timetable.length ? <div className="mini-schedule">{timetable.slice(0, 4).map((entry) => <div className="mini-entry" key={entry.timetableEntryId}><span>{entry.dayOfWeek?.slice(0, 3)}</span><b>{entry.courseCode}</b><small>{entry.startTime?.slice(0, 5)}</small></div>)}</div> : <div className="empty-inline"><p>{hasRegistration ? 'Your six registered courses are ready to be placed into a weekly timetable.' : 'Register for your courses first, then generate a timetable.'}</p>{hasRegistration && <button className="primary-button" onClick={onGenerate} disabled={busy}>{busy ? 'Working…' : 'Generate timetable ↗'}</button>}</div>}</section></div><div className="dashboard-tip"><span>✦</span><div><b>Your semester at a glance</b><p>Keep your course registration and weekly schedule up to date in one place.</p></div><span className="tip-term">{student.academicYear}</span></div></>
}
function StatCard({ icon, label, value, note }) { return <div className="stat-card"><div className="stat-top"><span className="stat-icon">{icon}</span><span className="stat-label">{label}</span></div><div className="stat-value">{value}</div><div className="stat-note">{note}</div></div> }
function CoursesView({ student, policy, courses, registered, selected, toggleCourse, registerCourses, busy }) {
  const [query, setQuery] = useState('')
  const [detailsCourse, setDetailsCourse] = useState(null)
  const registeredIds = new Set(registered.map((course) => course.courseId))
  const termRegistered = registered.filter((course) => courses.some((item) => item.courseId === course.courseId))
  const isRegistered = termRegistered.length === 6
  const visibleCourses = isRegistered ? courses.filter((course) => registeredIds.has(course.courseId)) : courses
  const filteredCourses = visibleCourses.filter((course) => `${course.courseName} ${course.courseCode}`.toLowerCase().includes(query.trim().toLowerCase()))
  const selectedCourses = courses.filter((course) => selected.includes(course.courseId))
  const selectedCredits = selectedCourses.reduce((total, course) => total + Number(course.credits || 0), 0)
  const registrationOpen = policy?.open !== false
  const deadlineText = formatPolicyDate(policy?.deadlineAt)
  const defaultCourseText = policy?.defaultCourses?.map((course) => course.courseCode).filter(Boolean).join(', ')
  return <section className="content-card full-card">
    <div className="card-heading"><div><span className="card-eyebrow">SEMESTER CATALOG</span><h2>{isRegistered ? 'Your registered courses' : 'Choose your courses'}</h2><p className="card-subtitle">{isRegistered ? 'Your current term registration is complete.' : `Choose six courses from the ${visibleCourses.length} courses in your current term.`}</p></div><span className="count-pill">{isRegistered ? `${termRegistered.length} registered` : `${selected.length} / 6 selected`}</span></div>
    {!isRegistered && <div className={`registration-reminder ${registrationOpen ? '' : 'registration-reminder-closed'}`}><span className="registration-reminder-icon" aria-hidden="true">{registrationOpen ? '!' : '✓'}</span><div className="registration-reminder-copy"><b>{registrationOpen ? 'Upcoming semester registration is not complete' : 'The registration deadline has passed'}</b><p>{registrationOpen ? <>Select any 6 courses you prefer for Semester {student.currentSemester}, academic year {student.academicYear}{deadlineText && <> by <strong>{deadlineText}</strong></>}. If you do not submit your choices by the deadline, the default courses will be allotted automatically.</> : 'Your default courses will be allotted automatically if you did not complete your selection before the deadline.'}</p>{defaultCourseText && <small>Default courses: {defaultCourseText}</small>}</div>{deadlineText && <span className="registration-deadline"><small>SELECTION DEADLINE</small><b>{formatPolicyDate(policy.deadlineAt, { short: true })}</b></span>}</div>}
    <div className="catalog-toolbar"><label className="course-search"><span aria-hidden="true">⌕</span><input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search by course name or code" aria-label="Search courses" />{query && <button type="button" onClick={() => setQuery('')} aria-label="Clear search">×</button>}</label><span className="catalog-count">Showing {filteredCourses.length} of {visibleCourses.length} courses</span></div>
    {!isRegistered && <div className="selection-summary"><div className="selection-progress-copy"><b>{selected.length} of 6 selected</b><span>{selectedCredits} credit{selectedCredits === 1 ? '' : 's'} selected</span></div><div className="selection-progress" role="progressbar" aria-valuenow={selected.length} aria-valuemin="0" aria-valuemax="6"><span style={{ width: `${Math.min(selected.length / 6 * 100, 100)}%` }} /></div></div>}
    {filteredCourses.length === 0 ? <p className="empty-message">{query ? 'No courses match your search.' : 'No courses were found for your department and term.'}</p> : <div className="course-table-wrap"><table className="course-table"><thead><tr><th>COURSE</th><th>CODE</th><th>FACULTY ID</th><th>CREDITS</th><th>COURSE TYPE</th><th>DETAILS</th>{isRegistered ? <th>STATUS</th> : <th>SELECT</th>}</tr></thead><tbody>{filteredCourses.map((course) => { const checked = selected.includes(course.courseId); const already = registeredIds.has(course.courseId); return <tr key={course.courseId}><td><div className="table-course"><span className="course-symbol">{course.courseCode?.slice(0, 2)}</span><b>{course.courseName}</b></div></td><td><span className="code-tag">{course.courseCode}</span></td><td>Faculty {course.facultyId}</td><td>{course.credits}</td><td><span className={`course-type-pill ${course.courseType === 'PRACTICAL' ? 'course-type-practical' : ''}`}>{course.courseType || '—'}</span></td><td><button type="button" className="course-details-button" onClick={() => setDetailsCourse(course)}>View units</button></td>{isRegistered ? <td><span className="registered-status">✓ Registered</span></td> : <td><label className="check-label"><input type="checkbox" checked={checked} disabled={already || (selected.length >= 6 && !checked)} onChange={() => toggleCourse(course.courseId)} aria-label={`Select ${course.courseName}`} />{already ? 'Added' : checked ? 'Selected' : 'Add'}</label></td>}</tr> })}</tbody></table></div>}
    {!isRegistered && <div className="register-bar"><p>{!registrationOpen ? 'The selection deadline has passed.' : selected.length === 6 ? 'Six courses selected. Submit when you are ready.' : `Select ${6 - selected.length} more course${selected.length === 5 ? '' : 's'} to continue.`}</p><button className="primary-button" disabled={busy || !registrationOpen || selected.length !== 6} onClick={registerCourses}>{busy ? 'Submitting…' : !registrationOpen ? 'Registration closed' : 'Register for selected courses ↗'}</button></div>}
    {detailsCourse && <CourseDetailsDialog course={detailsCourse} onClose={() => setDetailsCourse(null)} />}
  </section>
}

function CourseDetailsDialog({ course, onClose }) {
  const [details, setDetails] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => {
    let active = true
    api(`/api/courses/${course.courseId}/details`)
      .then((data) => { if (active) setDetails(data) })
      .catch((err) => { if (active) setError(err.message || 'Could not load this course syllabus.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [course.courseId])
  return <div className="course-dialog-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
    <section className="course-dialog" role="dialog" aria-modal="true" aria-labelledby="course-dialog-title">
      <header className="course-dialog-header"><div><span className="card-eyebrow">COURSE SYLLABUS</span><h2 id="course-dialog-title">{course.courseName}</h2><p>{course.courseCode} · {course.courseType || 'Course'} · {course.credits} credits</p></div><button type="button" onClick={onClose} aria-label="Close course details">×</button></header>
      <div className="course-dialog-content">{loading ? <div className="loading-panel"><span className="spinner dark-spinner" /> Loading course units…</div> : error ? <div className="empty-message" role="alert">{error}</div> : details?.units?.length ? details.units.map((unit) => <article className="syllabus-unit" key={unit.unitNumber}><div className="syllabus-unit-heading"><span>UNIT {unit.unitNumber}</span><h3>{unit.unitName}</h3></div><ul>{unit.topics.map((topic, index) => <li key={`${unit.unitNumber}-${index}`}>{topic}</li>)}</ul></article>) : <div className="empty-message">No unit details are available for this course yet.</div>}</div>
    </section>
  </div>
}

function formatClock(time) {
  if (!time) return ''
  const [hourText, minuteText = '00'] = time.split(':')
  const hour = Number(hourText)
  const suffix = hour >= 12 ? 'PM' : 'AM'
  return `${hour % 12 || 12}:${minuteText} ${suffix}`
}

function TimetableView({ timetable, onGenerate, busy }) {
  const startTimes = [...new Set(timetable.map((entry) => entry.startTime))].sort()
  const dailyCounts = DAYS.map((day) => ({ day, count: timetable.filter((entry) => entry.dayOfWeek === day).length }))
  const firstAfternoonRow = startTimes.findIndex((time) => Number(time.slice(0, 2)) >= 12)

  return <section className="content-card full-card timetable-panel">
    <div className="card-heading timetable-heading">
      <div><span className="card-eyebrow">WEEKLY SCHEDULE</span><h2>Class timetable</h2><p className="card-subtitle">Every meeting is shown in its own time slot. Afternoon times use AM/PM.</p></div>
      <button className="primary-button" disabled={busy} onClick={onGenerate}>{busy ? 'Generating…' : timetable.length ? 'Regenerate timetable ↻' : 'Generate timetable ↗'}</button>
    </div>
    {timetable.length ? <>
      <div className="timetable-summary" aria-label="Classes per weekday">
        {dailyCounts.map(({ day, count }) => <div className="timetable-day-count" key={day}><span>{day.slice(0, 3)}</span><b>{count}</b><small>{count === 1 ? 'class' : 'classes'}</small></div>)}
        <div className="weekly-total"><span>WEEKLY TOTAL</span><b>{timetable.length} <small>classes</small></b></div>
      </div>
      <div className="timetable-scroll"><div className="timetable-grid timetable-grid-dynamic">
        <div className="day-head time-head">TIME</div>{DAYS.map((day) => <div className="day-head" key={day}>{day}</div>)}
        {startTimes.map((startTime, index) => <Fragment key={startTime}>
          {index === firstAfternoonRow && <div className="lunch-break-row"><span>BREAK</span>Lunch break <small>12:15–1:15 PM</small></div>}
          <div className="time-column"><b>{formatClock(startTime)}</b></div>
          {DAYS.map((day) => <ScheduleCell key={`${startTime}-${day}`} entries={timetable.filter((entry) => entry.dayOfWeek === day && entry.startTime === startTime)} />)}
        </Fragment>)}
      </div></div>
    </> : <div className="empty-timetable"><div>◷</div><h3>No timetable yet</h3><p>Once you have registered for six courses, generate a timetable that fits faculty availability.</p><button className="primary-button" disabled={busy} onClick={onGenerate}>{busy ? 'Generating…' : 'Generate my timetable ↗'}</button></div>}
  </section>
}

function ScheduleCell({ entries }) {
  return <div className="schedule-cell">{entries.map((entry) => <article className={`timetable-course course-tone-${(Number(entry.courseId) - 1) % 6}`} key={entry.timetableEntryId}>
    <b>{entry.courseCode}</b><span>{entry.courseName}</span><small>{formatClock(entry.startTime)} – {formatClock(entry.endTime)}</small><small className="timetable-faculty">{entry.facultyName}</small>
  </article>)}</div>
}

function formatPolicyDate(value, { short = false } = {}) {
  if (!value) return ''
  const [year, month, day] = value.slice(0, 10).split('-').map(Number)
  return new Date(year, month - 1, day).toLocaleDateString('en-IN', {
    day: 'numeric', month: short ? 'short' : 'long', year: 'numeric',
  })
}

export default App









