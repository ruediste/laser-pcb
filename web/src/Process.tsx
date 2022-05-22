import { ChevronDownIcon, ChevronRightIcon } from '@primer/octicons-react';
import React, { useState } from 'react';
import { Button, Col, Container, Form, Row } from 'react-bootstrap';
import Select from 'react-dropdown-select';
import { toast } from 'react-toastify';
import CameraView from './CameraView';
import { Input, InputCheck } from './Inputs';
import JoggingControls from './JoggingControls';
import { SendGCode } from './SendGCode';
import { baseUrl, post, request } from './useData';
import { EditData } from './useEdit';
import WithData from './WithData';

interface UploadingFile {
    name: string;
    progress: number;
}

interface UploadedFileProps {
    file: PrintPcbInputFile
}

function NoClickBubble(props: React.PropsWithChildren<{}>) {
    return <span onClick={(e) => e.stopPropagation()}>{props.children} </span>;
}

function UploadedFile(props: UploadedFileProps) {
    const [open, setOpen] = useState(false);
    const { file } = props;
    return <li className="list-group-item">
        <div className="d-flex align-items-center" onClick={() => setOpen(o => !o)}>
            {open ? <ChevronDownIcon size={16} /> : <ChevronRightIcon size={16} />}
            <span className="mr-auto">{file.file.name}</span>
            <span>{file.file.status}</span>
            <NoClickBubble><Select options={[{ label: 'TOP', value: 'TOP' }, { label: 'BOTTOM', value: "BOTTOM" }, { label: 'DRILL', value: "DRILL" }]} values={file.file.layer === undefined ? [] : [{ label: file.file.layer, value: file.file.layer }]}
                onChange={(value) => { post('process/printPcb/file/' + file.file.id).body({ layer: value.length === 0 ? null : value[0].value }).success('Layer changed').send() }} /></NoClickBubble>
            <NoClickBubble><Button onClick={(e) => request('process/printPcb/file/' + file.file.id).method('DELETE').success('File ' + file.file.name + " removed").send()}>Remove</Button></NoClickBubble>
        </div>
        {
            open ? <div>
                {file.file.errorMessage !== undefined ? file.file.errorMessage : null}
                {file.inputSvgAvailable ? <img src={baseUrl + 'process/printPcb/file/' + file.file.id + '/input' + file.inputSvgHash + '.svg'} style={{ width: '50%' }} alt="Camera View" /> : null}
            </div> : null
        }
    </li >;
}
interface GerberFilesProps {
    uploadedFiles: PrintPcbInputFile[];
}

function GerberFiles(props: GerberFilesProps) {
    const [uploadingFiles, setUploadingFiles] = useState<UploadingFile[]>([]);

    return <Form>
        <div className="mb-3">
            <label className="form-label">Choose file</label>
            <input type="file" className="form-control" multiple onChange={e => {
                const files = e.target.files;
                if (files == null)
                    return;
                const tmp: UploadingFile[] = [];
                for (var i = 0; i < files.length; i++) {
                    const file = files.item(i);
                    if (file == null)
                        continue;
                    const uploadingFile = { name: file.name, progress: 0 };
                    tmp.push(uploadingFile);
                    post("process/printPcb/_addFile").bodyRaw(file).query({ name: file.name })
                        .success((data) => {
                            toast.success(file.name + " uploaded");
                            setUploadingFiles(old => old.filter(x => x !== uploadingFile));
                        })
                        .upload(({ loaded, total }) => {
                            console.log(loaded, total);
                            uploadingFile.progress = Math.round(100 * loaded / total);
                            setUploadingFiles(old => [...old]);
                        });
                }
                setUploadingFiles(old => [...old, ...tmp]);

                // clear the selected files
                e.target.value = '';
            }} />
        </div>

        {uploadingFiles.map((file, idx) =>
            <div key={idx} className="d-flex align-items-center">
                <span>{file.name}</span>
                <div className="progress flex-fill">
                    <div className="progress-bar" role="progressbar" style={{ "width": file.progress + "%" }} aria-valuenow={file.progress} aria-valuemin={0} aria-valuemax={100}></div>
                </div>
            </div>)}
        <ul className="list-group">
            {props.uploadedFiles.map((file) => <UploadedFile key={file.file.id} file={file} />)}
        </ul>
    </Form>;
}


interface PrintPcbInputFile {
    file: {
        name: string;
        errorMessage?: string;
        status: 'PARSING' | 'ERROR_PARSING' | 'PARSED' | 'PROCESSING' | 'PROCESSED' | 'ERROR_PROCESSING';
        layer?: string;
        id: string;
    }
    inputSvgAvailable: boolean;
    inputSvgHash: string;
    imageSvgAvailable: boolean;
    imageSvgHash: string;
    buffersSvgAvailable: boolean;
    buffersSvgHash: string;
}

interface PrintPcbProcess {
    status: 'INITIAL' | 'PROCESSING_FILES' | 'FILES_PROCESSED' | 'POSITION_1' | 'EXPOSING_1' | 'POSITION_2' | 'EXPOSING_2';
    inputFiles: PrintPcbInputFile[];
    processedFiles: PrintPcbInputFile[];
    readyToProcessFiles: boolean;
    userMessage: String;
}

function PrintPcb({ printPcb }: { printPcb: PrintPcbProcess }) {
    const [showImage, setShowImage] = useState<any>({});
    const imageList = [];
    {
        let nr = 0;
        for (let file of printPcb.processedFiles) {
            // if (showImage[file.file.layer + ' image'] !== false)
            imageList.push(<img key={nr} alt="PCB" src={baseUrl + 'process/printPcb/file/' + file.file.id + '/image' + file.imageSvgHash + '.svg'}
                style={{ width: '100%', maxHeight: '1000px', position: (nr++) === 0 ? 'relative' : 'absolute', left: 0, top: 0, display: showImage[file.file.layer + ' image'] !== false ? undefined : 'none', shapeRendering: 'optimizeSpeed' }} />)

            // if (showImage[file.file.layer + ' buffers'] !== false)
            imageList.push(<img key={nr} alt="Buffers" src={baseUrl + 'process/printPcb/file/' + file.file.id + '/buffers' + file.buffersSvgHash + '.svg'}
                style={{ width: '100%', maxHeight: '1000px', position: (nr++) === 0 ? 'relative' : 'absolute', left: 0, top: 0, display: showImage[file.file.layer + ' buffers'] !== false ? undefined : 'none', shapeRendering: 'optimizeSpeed' }} />)
        }
    }
    return <React.Fragment>
        {printPcb.status}
        <h1> Gerber Files</h1>
        <GerberFiles uploadedFiles={printPcb.inputFiles} />
        <Button disabled={!printPcb.readyToProcessFiles} onClick={() => post("process/printPcb/_processFiles").success('Process Files triggered').send()}>Process Files</Button><br />
        {printPcb.status !== 'FILES_PROCESSED' ? null : <React.Fragment>
            <Button onClick={() => post("process/printPcb/_startExposing").success('Start Exposing triggered').send()}>Start Exposing</Button><br />
            {printPcb.processedFiles.map((file, idx) => <React.Fragment key={idx}>
                <InputCheck style={{ display: 'inline-block' }} label={file.file.layer + ' image'} value={showImage[file.file.layer + ' image'] !== false} onChange={v => setShowImage({ ...showImage, ...{ [file.file.layer + ' image']: v } })} />{' '}
                <InputCheck style={{ display: 'inline-block' }} label={file.file.layer + ' buffers'} value={showImage[file.file.layer + ' buffers'] !== false} onChange={v => setShowImage({ ...showImage, ...{ [file.file.layer + ' buffers']: v } })} />{' '}
            </React.Fragment>)}
            <div style={{ position: 'relative' }}>
                {imageList}
            </div>
        </React.Fragment>}

        {printPcb.status !== 'POSITION_1' && printPcb.status !== 'POSITION_2' ? null : <React.Fragment>
            <p>{printPcb.userMessage}</p>
            <Button onClick={() => post("process/printPcb/_addPositionPoint").send()}>Add Positioning Point</Button>
            <div className="row">
                <div className="col-md-6"><JoggingControls /></div>
                <div className="col-md-6"><CameraView /> </div>
            </div>
        </React.Fragment>}
        {printPcb.status !== 'EXPOSING_1' && printPcb.status !== 'EXPOSING_2' ? null : <React.Fragment>
            <SendGCode />
        </React.Fragment>}
    </React.Fragment>
}

type LaserHeightCalibrationStep = 'PREPARE' | 'EXPOSE_PATTERN' | 'SET_HEIGHT';
interface LaserHeightCalibrationProcess {
    currentStep: LaserHeightCalibrationStep
    startHeight: number
    endHeight: number
    count: number
}
interface LaserHeightCalibrationProcessPMod {
    currentStep: LaserHeightCalibrationStep
    laserHeights: number[]
}
function LaserHeightCalibration({ process }: { process: LaserHeightCalibrationProcessPMod }) {
    const [z, setZ] = useState(0);
    const heights = process.laserHeights === null ? null : process.laserHeights.map(x => '' + x).join(',');
    return <div>
        step: {process.currentStep}<br />
        {process.currentStep !== 'PREPARE' ? null : <React.Fragment>
            <p>The laser height calibration will expose parallel lines at the distance configured as laser width using different laser Z heights,
                starting with the lowest configured height. After exposing, choose the Z height where the lines just touch (no gap).
            </p>
            <p>Use the camera to choose the bottom left corner of the exposed pattern</p>
            <EditData<LaserHeightCalibrationProcess> url="process/laserHeightCalibration">
                {([p, edit]) => p === undefined ? null : <React.Fragment>
                    <Input type="number" label="Start Laser Height [mm]" value={'' + p.startHeight} onChange={p => edit.update({ startHeight: parseFloat(p) })} />
                    <Input type="number" label="End Laser Height [mm]" value={'' + p.endHeight} onChange={p => edit.update({ endHeight: parseFloat(p) })} />
                    <Input type="number" label="Number of heights to expose" value={'' + p.count} onChange={p => edit.update({ count: parseInt(p) })} />
                    <Button onClick={() => edit.save()}>Save</Button>
                </React.Fragment>}
            </EditData>
            <JoggingControls />
            <br /> heights: {heights}<br />
            <Button onClick={() => post("process/laserHeightCalibration/_exposePattern").send()}>Expose Pattern</Button>
        </React.Fragment>}
        {process.currentStep !== 'EXPOSE_PATTERN' ? null : <SendGCode />}
        {process.currentStep !== 'SET_HEIGHT' ? null : <React.Fragment>
            <br /> heights: {heights}<br />
            <Input type="number" label="Set Laser Height [mm]" value={'' + z} onChange={p => setZ(parseFloat(p))} />
            <Button onClick={() => post("process/laserHeightCalibration/_setHeight").query({ laserHeight: '' + z }).error("Error while setting laser height").success("Laser Height Updated").send()}>Set Height</Button>
        </React.Fragment>}

    </div>;
}


interface CameraCalibrationProcess {
    currentStep: 'MOVE_TO_ORIGIN' | 'EXPOSE_CROSS' | 'POSITION_CAMERA';
}

function CameraCalibration({ cameraCalibration }: { cameraCalibration: CameraCalibrationProcess }) {
    return <div>
        step: {cameraCalibration.currentStep} <br />
        {
            cameraCalibration.currentStep !== 'MOVE_TO_ORIGIN' ? null : <React.Fragment>
                <p> The camera offset is calibrated by first exposing a cross, and then moving the crosshair of the camera
                    exactly to the position of the cross.</p>
                <p>First move the crosshair to the location where the cross should be exposed. This uses
                    the offsets currently configured. If you configure this for the first time, just set
                    the offset to 0 and position the laser, ignoring the camera position.
                </p>
                <Container fluid>
                    <Row>
                        <Col><JoggingControls /></Col>
                        <Col><CameraView /></Col>
                    </Row>
                </Container>
                <Button onClick={() => post("process/cameraCalibration/exposeCross").send()}>Expose Cross</Button>
            </React.Fragment>
        }{
            cameraCalibration.currentStep !== 'EXPOSE_CROSS' ? null : <React.Fragment>
                <SendGCode />
            </React.Fragment>
        }
        {
            cameraCalibration.currentStep !== 'POSITION_CAMERA' ? null : <React.Fragment>
                <JoggingControls />
                <CameraView />
                <Button onClick={() => post("process/cameraCalibration/applyOffset").send()}>Apply Offset</Button>
            </React.Fragment>
        }
    </div>;
}

interface Process {
    printPcb: PrintPcbProcess;
    laserHeightCalibration: LaserHeightCalibrationProcessPMod;
    cameraCalibration: CameraCalibrationProcess;
}

export default function ProcessComponent() {

    return <React.Fragment>
        <h1> Process </h1>
        <Button onClick={() => post("process/printPcb/launch").send()}>Launch Print PCB Process</Button>
        <WithData<Process> url="process"
            refreshMs={1000}
            render={(process) => {
                let { printPcb, cameraCalibration, laserHeightCalibration } = process;
                if (printPcb !== null) return <PrintPcb printPcb={printPcb} />;
                if (cameraCalibration !== null) return <CameraCalibration cameraCalibration={cameraCalibration} />;
                if (laserHeightCalibration !== null) return <LaserHeightCalibration process={laserHeightCalibration} />;

                return null;
            }} />
    </React.Fragment>;
}
