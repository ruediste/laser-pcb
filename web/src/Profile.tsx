import WithData from './WithData'
import React, { useState, useEffect } from 'react';
import { request, post, useRefreshTrigger } from './useData';
import { toast } from 'react-toastify';
import Select from 'react-dropdown-select';
import { Row, Col, Button, Card, Collapse, OverlayTrigger, Tooltip } from 'react-bootstrap';
import { InputCheck, Input } from './Inputs';
import { useNavigate } from 'react-router-dom';
import useEdit from './useEdit';
import { InfoIcon } from '@primer/octicons-react';

type Corner = 'BL' | 'BR';

interface Profile {
    id: string;
    name: string;
    singleLayerPcb: boolean;
    laserZ: number;
    exposureOverlap: number;
    baudRate: number;

    exposureWidth: number;
    exposureFeed: number;
    fastMovementFeed: number;

    laserIntensity: number;
    preExposeGCode: string;

    bedSizeX: number;
    bedSizeY: number;

    cameraRotation: number;
    cameraOffsetX: number;
    cameraOffsetY: number;
    cameraZ: number;
    preferredAlignmentCorner: Corner;

    minDrillSize: number;
    maxDrillSize: number;
    drillOffset: number;
    drillScale: number;

    boardBorder: number;
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

function Info({ children }: { children: React.ReactNode }) {
    return <OverlayTrigger overlay={<Tooltip>{children}</Tooltip>}>
        <><InfoIcon /></>
    </OverlayTrigger >
}
export default function ProfileComponent() {
    const navigate = useNavigate();
    const [selectedProfile, setSelectedProfile] = useState<Profile[]>([]);
    const [profile, editProfile] = useEdit<Profile>(selectedProfile.length === 0 ? undefined : 'profile/' + selectedProfile[0].id);
    const profilesRefreshTrigger = useRefreshTrigger();
    useEffect(() => request('profile/current').success(profile => { if (profile !== null) setSelectedProfile([profile]) }).send(), []);
    return <React.Fragment>
        <h1> Profile </h1>
        <div className="container-fluid">

            <WithData<Profile[]> url="profile" trigger={profilesRefreshTrigger} render={(data, trigger) =>
                <Row style={{ marginBottom: '16px' }}>
                    <Col md>
                        <Select options={data} values={selectedProfile} onChange={value => {
                            setSelectedProfile(value);
                            if (value.length > 0) post('profile/current').query({ id: value[0].id }).send();
                        }} labelField="name" valueField="id" />
                    </Col>
                    <Col md>
                        <Button onClick={() => post("profile/" + selectedProfile[0].id + "/_copy").success((p) => { setSelectedProfile([p]); toast.success('Profile Copied'); trigger() }).send()} disabled={selectedProfile.length === 0}> Copy </Button>{" "}
                        <Button onClick={() => post("profile").body({ name: 'New Profile' }).success((p) => { setSelectedProfile([p]); toast.success('Profile Created'); trigger() }).send()}> New </Button>{" "}
                        <Button onClick={() => post("profile/_reload").success(() => { toast.success('Profiles Reloaded'); trigger() }).send()}>Reload Profiles</Button>{" "}
                        <Button onClick={() => request("profile/" + selectedProfile[0].id).method('DELETE').success(() => { setSelectedProfile([]); toast.success('Profile Deleted'); trigger() }).send()} variant="danger" disabled={selectedProfile.length === 0}>Delete</Button>{" "}
                        {profile === undefined ? null : <Button onClick={() => editProfile.save(() => { profilesRefreshTrigger.trigger(); setSelectedProfile([profile]) })}>Save</Button>}
                    </Col>
                </Row>
            } />

            {profile === undefined ? null : <React.Fragment>
                <Card className='mb-3'>
                    <Card.Title >General</Card.Title>
                    <Card.Body>
                        <Row>
                            <Col md>
                                <Input type="text" label="Profile Name" value={profile.name} onChange={value => editProfile.update({ name: value })} />
                                <Input type="number" label="Baud Rate (115200, 250000)" value={'' + profile.baudRate} onChange={p => editProfile.update({ baudRate: parseInt(p) })} />
                                <Input type="number" label="X Bed Size [mm]" value={'' + profile.bedSizeX} onChange={p => editProfile.update({ bedSizeX: parseFloat(p) })} />
                                <Input type="number" label="Y Bed Size [mm]" value={'' + profile.bedSizeY} onChange={p => editProfile.update({ bedSizeY: parseFloat(p) })} />
                            </Col>
                            <Col md>
                                <InputCheck label="Single Layer PCB" value={profile.singleLayerPcb} onChange={value => editProfile.update({ singleLayerPcb: value })} />
                                <Input type="number" label="Fast Movement Feed [mm/m]" value={'' + profile.fastMovementFeed} onChange={p => editProfile.update({ fastMovementFeed: parseFloat(p) })} />
                                <label > Preferred Alignment Corner <Info> For single layer PCBs only this corner (Bottom Left or Bottom Right) is used. For double layer PCBs the other corner is used for the TOP layer and this corner for the bottom layer </Info></label>
                                <Select<{ id: Corner }> options={[{ id: 'BL' }, { id: 'BR' }]} values={profile.preferredAlignmentCorner === undefined ? [] : [{ id: profile.preferredAlignmentCorner }]} onChange={p => {
                                    if (p.length > 0)
                                        editProfile.update({ preferredAlignmentCorner: p[0].id });
                                }} labelField="id" valueField="id" />
                                <Input type="number" label="Border around the board, from the reference point [mm]" value={'' + profile.boardBorder} onChange={p => editProfile.update({ boardBorder: parseFloat(p) })} />
                            </Col>
                        </Row>
                    </Card.Body>
                </Card>
                <Card className='mb-3'>
                    <Card.Title >Laser</Card.Title>
                    <Card.Body>
                        <Row>
                            <Col md>
                                <Input type="number" label="Laser Z [mm]" value={'' + profile.laserZ} onChange={p => editProfile.update({ laserZ: parseFloat(p) })} />
                                <Button onClick={() => post("process/laserHeightCalibration/_start").error("Error while starting calibration").success(() => navigate("/process")).send()}>Start Calibration</Button>

                                <Input type="number" label="Exposure Width [mm]" comment="Width of the line exposed" value={'' + profile.exposureWidth} onChange={p => editProfile.update({ exposureWidth: parseFloat(p) })} />
                                <Input type="number" label="Exposure Feed [mm/m]" comment="Feed to use during laser exposure" value={'' + profile.exposureFeed} onChange={p => editProfile.update({ exposureFeed: parseFloat(p) })} />
                                <Input type="number" label="Overlap between exposures (fraction of exposure width)" value={'' + profile.exposureOverlap} onChange={p => editProfile.update({ exposureOverlap: parseFloat(p) })} />
                            </Col>
                            <Col md>
                                <Input type="number" label="Laser Intensity [0..255]" comment="Intensity of the laser. 0 is off, 255 is 100%"  
                                step={1} min={0} max={255}
                                value={'' + profile.laserIntensity} onChange={p => editProfile.update({ laserIntensity: parseInt(p) })} />
                                <Button onClick={() => post("process/laserIntensityCalibration/_start").error("Error while starting calibration").success(() => navigate("/process")).send()}>Start Calibration</Button>
                                <Input type="textarea" label="Pre Expose GCode" value={profile.preExposeGCode} onChange={p => editProfile.update({ preExposeGCode: p })} />
                            </Col>
                        </Row>
                    </Card.Body>
                </Card>
                <Card className='mb-3'>
                    <Card.Title >Camera</Card.Title>
                    <Card.Body>
                        <Row>
                            <Col md>
                                {[0, 90, 180, 270].map((v, idx) => <div key={idx} className="form-check form-check-inline">
                                    <input className="form-check-input" type="radio" name="flexRadioDefault" id={"cameraRotate" + idx} checked={profile.cameraRotation === v} onChange={p => editProfile.update({ cameraRotation: v })} />
                                    <label className="form-check-label" htmlFor={"cameraRotate" + idx}>Camera Rotation {v}</label>
                                </div>)}
                                <Input type="number" label="Camera Z [mm]" value={'' + profile.cameraZ} onChange={p => editProfile.update({ cameraZ: parseFloat(p) })} />
                            </Col>
                            <Col md>
                                <Input type="number" label="CameraOffset X [mm]" value={'' + profile.cameraOffsetX} onChange={p => editProfile.update({ cameraOffsetX: parseFloat(p) })} />
                                <Input type="number" label="CameraOffset Y [mm]" value={'' + profile.cameraOffsetY} onChange={p => editProfile.update({ cameraOffsetY: parseFloat(p) })} />
                                <Button onClick={() => post("process/cameraCalibration/start").error("Error while starting calibration").success(() => navigate("/process")).send()}>
                                    Start Calibration</Button>
                            </Col>
                        </Row>
                    </Card.Body>
                </Card>
                <Card className='mb-3'>
                    <Card.Title >Drill</Card.Title>
                    <Card.Body>
                        The drill hole size is first scaled and then an offset is subtracted. If the resulting size is clamped to the min/max drill size.
                        <Row>
                            <Col md>
                                <Input type="number" label="Drill Scale [1]" value={'' + profile.drillScale} onChange={p => editProfile.update({ drillScale: parseFloat(p) })} />
                                <Input type="number" label="Drill Offset [mm]" value={'' + profile.drillOffset} onChange={p => editProfile.update({ drillOffset: parseFloat(p) })} />
                            </Col>
                            <Col md>
                                <Input type="number" label="Min Drill Size [mm]" value={'' + profile.minDrillSize} onChange={p => editProfile.update({ minDrillSize: parseFloat(p) })} />
                                <Input type="number" label="Max Drill Size [mm]" value={'' + profile.maxDrillSize} onChange={p => editProfile.update({ maxDrillSize: parseFloat(p) })} />
                            </Col>
                        </Row>
                    </Card.Body>
                </Card>
            </React.Fragment>}
        </div>
    </React.Fragment>
}
