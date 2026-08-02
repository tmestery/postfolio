import { BrowserRouter, Routes as BrowserRoutes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './auth/useAuth'
import Layout from './components/Layout'
import HomePage from './pages/home'
import LoginPage from './pages/login'
import SignupPage from './pages/signup'
import NewPostPage from './pages/newpost'
import AgentPage from './pages/agent'
import AccountPage from './pages/account'
import ProfilePage from './pages/profile'
import PeoplePage from './pages/people'
import NotificationsPage from './pages/notifications'
import NotFoundPage from './pages/notfound'

function RequireAuth({ children }) {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? children : <Navigate to="/login" replace />
}

export default function Routes() {
  return (
    <BrowserRouter>
      <Layout>
        <BrowserRoutes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/u/:username" element={<ProfilePage />} />
          <Route
            path="/post/new"
            element={
              <RequireAuth>
                <NewPostPage />
              </RequireAuth>
            }
          />
          <Route
            path="/people"
            element={
              <RequireAuth>
                <PeoplePage />
              </RequireAuth>
            }
          />
          <Route
            path="/notifications"
            element={
              <RequireAuth>
                <NotificationsPage />
              </RequireAuth>
            }
          />
          <Route
            path="/agent"
            element={
              <RequireAuth>
                <AgentPage />
              </RequireAuth>
            }
          />
          <Route
            path="/account"
            element={
              <RequireAuth>
                <AccountPage />
              </RequireAuth>
            }
          />
          <Route path="*" element={<NotFoundPage />} />
        </BrowserRoutes>
      </Layout>
    </BrowserRouter>
  )
}
