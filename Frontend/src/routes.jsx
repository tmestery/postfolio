import {BrowserRouter, Routes as BrowserRoutes, Route} from 'react-router-dom'

import HomePage from './pages/home'
import LoginPage from './pages/login'
import SignupPage from './pages/signup'
import NotFoundPage from './pages/notfound'

export default function Routes(){

    return(
        <BrowserRouter>
            <BrowserRoutes>
                <Route path="/" element={<HomePage />}/>
                <Route path="/login" element={<LoginPage />}/>
                <Route path="/signup" element={<SignupPage />}/>


            </BrowserRoutes>
        </BrowserRouter>
    )
}