import Routes from './routes.jsx'
import { AuthProvider } from './auth/AuthContext'

export default function App() {
  return (
    <AuthProvider>
      <Routes />
    </AuthProvider>
  )
}
