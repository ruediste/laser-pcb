import WithData from './WithData'
import React, { useState, useEffect } from 'react';
import { request, post, useRefreshTrigger } from './useData';
import { toast } from 'react-toastify';
import Select from 'react-dropdown-select';
import { Row, Col, Button, Card, Collapse } from 'react-bootstrap';
import { InputCheck, Input } from './Inputs';
import {
    useHistory
} from 'react-router-dom';
import useEdit from './useEdit';



interface Profile {
    id: string;
    name: string;
    singleLayerPcb: boolean;
    laserPower: number;
    laserDotSize: number;
    laserZ: number;
    exposureOverlap: number;
    baudRate: number;

    exposureWidth: number;
    exposureFeed: number;
    fastMovementFeed: number;

    laserOn: string;
    laserOff: string;
    preExposeGCode: string;

    bedSizeX: number;
    bedSizeY: number;

    cameraRotation: number;
    cameraOffsetX: number;
    cameraOffsetY: number;
    cameraZ: number;


}

interface CollapseCardProps {
    title: string;
    children: React.ReactNode;
}
function CollapseCard(props: CollapseCardProps) {
    const [open, setOpen] = useState(false);
    return <Card>
        <Card.Header onClick={() => setOpen(!open)}>{props.title}</Card.Header>
        <Collapse in={open}>
            <Card.Body> {props.children} </Card.Body>
        </Collapse>
    </Card>;
}

function LaserCalibration() {
    const [v1, setV1] = useState(100);
    const [v2, setV2] = useState(200);
    const history = useHistory();
    return <CollapseCard title="Calibration">
        The laser can be calibrated by exposing a line at two different speeds and then measuring the two line widths.
        <Input type="number" label="Speed 1" value={'' + v1} onChange={p => setV1(parseFloat(p))} />
        <Input type="number" label="Speed 2" value={'' + v2} onChange={p => setV2(parseFloat(p))} />
        <Button onClick={() => post("process/laserCalibration/start").query({ v1: '' + v1, v2: '' + v2 }).error("Error while starting calibration").success(() => history.push("/process")).send()}>
            Start Calibration</Button>
    </CollapseCard>;
}

export default function ProfileComponent() {
    const history = useHistory();
    const [selectedProfile, setSelectedProfile] = useState<Profile[]>([]);
    const [profile, editProfile] = useEdit<Profile>(selectedProfile.length === 0 ? undefined : 'profile/' + selectedProfile[0].id);
    const profilesRefreshTrigger = useRefreshTrigger();
    useEffect(() => request('profile/current').success(profile => { if (profile !== null) setSelectedProfile([profile]) }).send(), []);
    return <React.Fragment>
        <h1> Profile </h1>
        <div className="container-fluid">

            <WithData<Profile[]> url="profile" trigger={profilesRefreshTrigger} render={(data, trigger) =>
                <Row>
                    <Col>
                        <Select options={data} values={selectedProfile} onChange={value => {
                            setSelectedProfile(value);
                            if (value.length > 0) post('profile/current').query({ id: value[0].id }).send();
                        }} labelField="name" valueField="id" />
                    </Col>
                    <Col>
                        <Button onClick={() => post("profile/" + selectedProfile[0].id + "/_copy").success((p) => { setSelectedProfile([p]); toast.success('Profile Copied'); trigger() }).send()} disabled={selectedProfile.length === 0}> Copy </Button>{" "}
                        <Button onClick={() => post("profile").body({ name: 'New Profile' }).success((p) => { setSelectedProfile([p]); toast.success('Profile Created'); trigger() }).send()}> New </Button>{" "}
                        <Button onClick={() => post("profile/_reload").success(() => { toast.success('Profiles Reloaded'); trigger() }).send()}>Reload Profiles</Button>{" "}
                        <Button onClick={() => request("profile/" + selectedProfile[0].id).method('DELETE').success(() => { setSelectedProfile([]); toast.success('Profile Deleted'); trigger() }).send()} variant="danger" disabled={selectedProfile.length === 0}>Delete</Button>{" "}
                    </Col>
                </Row>
            } />

            {profile === undefined ? null : <React.Fragment>
                <Button onClick={() => editProfile.save(() => { profilesRefreshTrigger.trigger(); setSelectedProfile([profile]) })}>Save</Button>
                <input type="text" className="form-control" placeholder="Profile Name" value={profile.name} onChange={e => editProfile.update({ name: e.target.value })} />
                <InputCheck label="Single Layer PCB" value={profile.singleLayerPcb} onChange={value => editProfile.update({ singleLayerPcb: value })} />
                <Input type="number" label="X Bed Size [mm]" value={'' + profile.bedSizeX} onChange={p => editProfile.update({ bedSizeX: parseFloat(p) })} />
                <Input type="number" label="Y Bed Size [mm]" value={'' + profile.bedSizeY} onChange={p => editProfile.update({ bedSizeY: parseFloat(p) })} />
                <Input type="number" label="Fast Movement Feed [mm/m]" value={'' + profile.fastMovementFeed} onChange={p => editProfile.update({ fastMovementFeed: parseFloat(p) })} />
                <Input type="number" label="Baud Rate (115200, 250000)" value={'' + profile.baudRate} onChange={p => editProfile.update({ baudRate: parseInt(p) })} />

                <Card>
                    <Card.Title >Laser</Card.Title>
                    <Card.Body>
                        <Input type="number" label="Laser Power [m^2/s]" value={'' + profile.laserPower} onChange={p => editProfile.update({ laserPower: parseFloat(p) })} />
                        <Input type="number" label="Laser Dot Size (diameter, [mm])" value={'' + profile.laserDotSize} onChange={p => editProfile.update({ laserDotSize: parseFloat(p) })} />
                        <Input type="number" label="Laser Z [mm]" value={'' + profile.laserZ} onChange={p => editProfile.update({ laserZ: parseFloat(p) })} />
                        <Button onClick={() => post("process/laserHeightCalibration/_start").error("Error while starting calibration").success(() => history.push("/process")).send()}>Start Calibration</Button>

                        <Input type="number" label="Overlap between exposures (fraction of exposure width)" value={'' + profile.exposureOverlap} onChange={p => editProfile.update({ exposureOverlap: parseFloat(p) })} />

                        <Input type="number" label="Exposure Width [mm]" comment="temporary exposure setting" value={'' + profile.exposureWidth} onChange={p => editProfile.update({ exposureWidth: parseFloat(p) })} />
                        <Input type="number" label="Exposure Feed [mm/m]" comment="temporary exposure setting" value={'' + profile.exposureFeed} onChange={p => editProfile.update({ exposureFeed: parseFloat(p) })} />

                        <Input type="text" label="Laser On" value={profile.laserOn} onChange={p => editProfile.update({ laserOn: p })} />
                        <Input type="text" label="Laser Off" value={profile.laserOff} onChange={p => editProfile.update({ laserOff: p })} />
                        <Input type="textarea" label="Pre Expose GCode" value={profile.preExposeGCode} onChange={p => editProfile.update({ preExposeGCode: p })} />
                        <LaserCalibration />
                    </Card.Body>
                </Card>
                <Card>
                    <Card.Title >Camera</Card.Title>
                    <Card.Body>
                        {[0, 90, 180, 270].map((v, idx) => <div key={idx} className="form-check form-check-inline">
                            <input className="form-check-input" type="radio" name="flexRadioDefault" id={"cameraRotate" + idx} checked={profile.cameraRotation === v} onChange={p => editProfile.update({ cameraRotation: v })} />
                            <label className="form-check-label" htmlFor={"cameraRotate" + idx}>Camera Rotation {v}</label>
                        </div>)}
                        <Input type="number" label="CameraOffset X [mm]" value={'' + profile.cameraOffsetX} onChange={p => editProfile.update({ cameraOffsetX: parseFloat(p) })} />
                        <Input type="number" label="CameraOffset Y [mm]" value={'' + profile.cameraOffsetY} onChange={p => editProfile.update({ cameraOffsetY: parseFloat(p) })} />
                        <Input type="number" label="Camera Z [mm]" value={'' + profile.cameraZ} onChange={p => editProfile.update({ cameraZ: parseFloat(p) })} />
                        <Button onClick={() => post("process/cameraCalibration/start").error("Error while starting calibration").success(() => history.push("/process")).send()}>
                            Start Calibration</Button>
                    </Card.Body>
                </Card>
            </React.Fragment>}
        </div>
    </React.Fragment>
}
