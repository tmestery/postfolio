import {BrowserRouter, Routes as BrowserRoutes, Route} from 'react-router-dom'

import HomePage from './pages/home'
import LoginPage from './pages/login'

export default function Routes(){

    return(
        <BrowserRouter>
            <BrowserRoutes>
                <Route path="/" element={<HomePage />}/>
                <Route path="/login" element={<LoginPage />}/>
            </BrowserRoutes>
        </BrowserRouter>
    )
}