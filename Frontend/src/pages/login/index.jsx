import {useState} from 'react'


export default function LoginPage(){
    const [loginInfo, setLoginInfo] = useState({
        username: "",
        email: "",
        password: ""
    }) 


    return (
        <div>
            <h1 className="text-large">Create your account</h1>
            <form>
                <label>Username</label>
                {/* <input type="text" name="username" onChange={e=>setLoginInfo(li => {{...li, username: e.target.value}})} /> */}
            </form>
        </div>
        
    )
}