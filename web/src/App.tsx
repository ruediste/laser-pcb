import './App.css';
import React, { useEffect } from 'react';
import { Navbar, Nav } from 'react-bootstrap';
import {
  BrowserRouter as Router,
  Switch,
  Route,
  Link, useLocation
} from 'react-router-dom';
import Profile from './Profile';
import Process from './Process';
import Control from './Control';
import { ToastContainer } from 'react-toastify';

import 'react-toastify/dist/ReactToastify.css';

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
    <Navbar.Brand href="#home">Laser PCB</Navbar.Brand>
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
            <e.component />
          </Route>
        )}
      </Switch>
      <ToastContainer hideProgressBar={true} />
    </Router>
  );
}

export default App;
