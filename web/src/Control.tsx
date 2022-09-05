import { useState } from "react";
import { Button, Row, Col, Card } from "react-bootstrap";
import Select from "react-dropdown-select";
import CameraView from "./CameraView";
import { Input } from "./Inputs";
import { JoggingControlsNoLoad, SerialConnections } from "./JoggingControls";
import { post } from "./useData";
import WithData from "./WithData";



export default function ControlComponent() {
    const [script, setScript]=useState('');
    return <div className="container-fluid">
        <WithData<SerialConnections> url="cncConnection"
            refreshMs={500}
            render={state => <Row>
                <Col md>
                    <Card>
                        <Card.Title >CNC Connection</Card.Title>
                        <Card.Body>
                            <Select options={state.availableSerialConnections.map((x) => ({ label: x, value: x }))} values={state.selectedSerialConnection === undefined || state.selectedSerialConnection === null ? [] : [{ label: state.selectedSerialConnection, value: state.selectedSerialConnection }]}
                                onChange={(value) => { post('cncConnection/_setSerialConnection').query({ dev: value[0].value }).success('Serial Connection Changed').send() }} />
                            <Button onClick={() => post('cncConnection/_connect').success("Connection initiated").send()}>Connect</Button>
                            <Button onClick={() => post('cncConnection/_disconnect').success("Disconnected").send()}>Disconnect</Button>

                            <JoggingControlsNoLoad state={state} />
                            {!state.serialConnected?null:<>
                            <Input type="textarea" label="Script" value={script} onChange={setScript} rows={10}/>
                            <Button onClick={() => post('cncConnection/_script').bodyRaw(new Blob([script])).success("Disconnected").send()}>Send</Button>

                            </>}
                        </Card.Body>
                    </Card>
                </Col>
                <Col md>
                    <Card>
                        <Card.Title >Video Connection</Card.Title>
                        <Card.Body>
                            <Select options={state.availableVideoConnections.map((x) => ({ label: x, value: x }))} values={state.selectedVideoConnection === undefined || state.selectedVideoConnection === null ? [] : [{ label: state.selectedVideoConnection, value: state.selectedVideoConnection }]}
                                onChange={(value) => { post('cncConnection/_setVideoConnection').query({ dev: value[0].value }).success('Video Connection Changed').send() }} />

                            <CameraView />
                        </Card.Body>
                    </Card>
                </Col>
            </Row>} />
    </div>;
}