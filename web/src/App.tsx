import React from 'react';
import { Nav, Navbar } from 'react-bootstrap';
import {
  BrowserRouter as Router,
  Link, Route, Switch,
  useLocation
} from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import './App.css';
import Control from './Control';
import Process from './Process';
import Profile from './Profile';


interface NavEntry {
  title: string;
  path: string;
  component: React.FunctionComponent
}

let navEntries: NavEntry[] = [
  { title: 'Profile', path: '/profile', component: Profile },
  { title: 'Process', path: '/process', component: Process },
  { title: 'Control', path: '/control', component: Control },
];

function Navigation() {
  let location = useLocation();
  return <Navbar bg="light" expand="lg">
    <Link className="navbar-brand" to="/process">Laser PCB</Link>
    <Navbar.Toggle aria-controls="basic-navbar-nav" />
    <Navbar.Collapse id="basic-navbar-nav">
      <Nav className="mr-auto">
        {navEntries.map((e, idx) => <Link key={idx} className={"nav-link" + (location.pathname === e.path ? " active" : "")} to={e.path}>{e.title}</Link>)}
      </Nav>
    </Navbar.Collapse>
  </Navbar>;
}

function App() {
  return (
    <Router>
      <Navigation />
      <Switch>
        {navEntries.map((e, idx) =>
          <Route key={idx} path={e.path}>
            <div className="container-fluid">
              <e.component />
            </div>
          </Route>
        )}
      </Switch>
      <ToastContainer hideProgressBar={true} />
    </Router>
  );
}

export default App;
