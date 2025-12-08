import {useState} from 'react'
import {useNavigate} from 'react-router-dom'

export default function SignupPage(){
    const [signupInfo, setSignupInfo] = useState({
        first_name: "",
        last_name: "",
        email: "",
        username: "",
        password: "",
        dob_month: "",
        dob_day: "",
        dob_year: ""
    }) 
    const navigate = useNavigate();


    async function handleSubmit(e){
        e.preventDefault()
        try{
            const response = await fetch('http://8080/credentials/login/', {
                headers: {"Content-Type": "application/json"},
                method: "POST",
                body: JSON.stringify(signupInfo)
            })
            if (response.ok) {
                navigate('/login');
            } else {
                alert('Signup failed');
            }
        } catch(err){
            console.error(err.message)
        }
    }
    
    
    return (
        <div>
            <form onSubmit={handleSubmit} className="w-200 align-center">
                <div className="flex flex-col text-left justify-center">
                    <label>Email</label>
                    <input
                    id="email"
                    type="text"
                    placeholder="Enter your email"
                    value={signupInfo.email}
                    onChange={e => setSignupInfo({...signupInfo, email: e.target.value})}
                    required
                    />
                </div>

                <div className="flex flex-col text-left">
                    <label>Username</label>
                    <input
                    id="username"
                    type="text"
                    placeholder="Enter your username"
                    value={signupInfo.username}
                    onChange={e => setSignupInfo({...signupInfo, username: e.target.value})}
                    required
                    />
                </div>

                <div className="flex flex-col text-left">
                    <label>Password</label>
                    <input
                    id="password"
                    type="password"
                    placeholder="Enter your password"
                    value={signupInfo.password}
                    onChange={e => setSignupInfo({...signupInfo, password: e.target.value})}
                    required
                    />
                </div>

                <button type="submit">Sign Up</button>
            </form>
        </div>
    )
}