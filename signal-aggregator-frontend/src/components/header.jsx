import React  from 'react';
import { Link } from 'react-router';

const Header = () => {
    return (
        <nav className="navbar navbar-toggleable-md navbar-inverse bg-inverse">
            <div className="collapse navbar-collapse">
                <ul className="navbar-nav mr-auto">
                    <li className="nav-item"><Link className="nav-link" to="/">Home</Link></li>
                    <li className="nav-item"><Link className="nav-link" to="/signals">Signals</Link></li>
                    <li className="nav-item"><Link className="nav-link" to="/aggregations">Aggregations</Link></li>
                    <li className="nav-item"><Link className="nav-link" to="/providers">Providers</Link></li>
                </ul>
            </div>
        </nav>
    )
}

export default Header