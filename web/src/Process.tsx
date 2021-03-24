import React, { useState } from 'react';
import { Button, Form } from 'react-bootstrap';
import Select from 'react-dropdown-select';
import { post, request, baseUrl } from './useData';
import { toast } from 'react-toastify';
import WithData from './WithData';
import { ChevronDownIcon, ChevronRightIcon } from '@primer/octicons-react';
import { InputCheck } from './Inputs';

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
            <NoClickBubble><Select options={[{ label: 'TOP', value: 'TOP' }, { label: 'BOTTOM', value: "BOTTOM" }]} values={file.file.layer === undefined ? [] : [{ label: file.file.layer, value: file.file.layer }]}
                onChange={(value) => { post('process/printPcb/file/' + file.file.id).body({ layer: value.length == 0 ? null : value[0].value }).success('Layer changed').send() }} /></NoClickBubble>
            <NoClickBubble><Button onClick={(e) => request('process/printPcb/file/' + file.file.id).method('DELETE').success('File ' + file.file.name + " removed").send()}>Remove</Button></NoClickBubble>
        </div>
        {
            open ? <div>
                {file.file.errorMessage !== undefined ? file.file.errorMessage : null}
                {file.inputSvgAvailable ? <img src={baseUrl + 'process/printPcb/file/' + file.file.id + '/input' + file.inputSvgHash + '.svg'} style={{ width: '50%' }} /> : null}
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
        <div className="custom-file">
            <input type="file" className="custom-file-input" multiple onChange={e => {
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
            <label className="custom-file-label">Choose file</label>
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
    status: 'INITIAL' | 'PROCESSING_FILES' | 'EXPOSING';
    inputFiles: PrintPcbInputFile[];
    processedFiles: PrintPcbInputFile[];
    readyToProcessFiles: boolean;
}

function PrintPcb({ printPcb }: { printPcb: PrintPcbProcess }) {
    const [showImage, setShowImage] = useState<any>({});
    const imageList = [];
    {
        let nr = 0;
        for (let file of printPcb.processedFiles) {
            // if (showImage[file.file.layer + ' image'] !== false)
            imageList.push(<img key={nr} src={baseUrl + 'process/printPcb/file/' + file.file.id + '/image' + file.imageSvgHash + '.svg'}
                style={{ width: '100%', position: (nr++) == 0 ? 'relative' : 'absolute', left: 0, top: 0, display: showImage[file.file.layer + ' image'] !== false ? undefined : 'none', shapeRendering: 'optimizeSpeed' }} />)

            // if (showImage[file.file.layer + ' buffers'] !== false)
            imageList.push(<img key={nr} src={baseUrl + 'process/printPcb/file/' + file.file.id + '/buffers' + file.buffersSvgHash + '.svg'}
                style={{ width: '100%', position: (nr++) == 0 ? 'relative' : 'absolute', left: 0, top: 0, display: showImage[file.file.layer + ' buffers'] !== false ? undefined : 'none', shapeRendering: 'optimizeSpeed' }} />)
        }
    }
    return <React.Fragment>
        {printPcb.status}
        <h1> Gerber Files</h1>
        <GerberFiles uploadedFiles={printPcb.inputFiles} />
        <Button disabled={!printPcb.readyToProcessFiles} onClick={() => post("process/printPcb/_processFiles").success('Process Files triggered').send()}>Process Files</Button><br />
        {printPcb.status !== 'EXPOSING' ? null : <React.Fragment>
            {printPcb.processedFiles.map((file, idx) => <React.Fragment key={idx}>
                <InputCheck style={{ display: 'inline-block' }} label={file.file.layer + ' image'} value={showImage[file.file.layer + ' image'] !== false} onChange={v => setShowImage({ ...showImage, ...{ [file.file.layer + ' image']: v } })} />{' '}
                <InputCheck style={{ display: 'inline-block' }} label={file.file.layer + ' buffers'} value={showImage[file.file.layer + ' buffers'] !== false} onChange={v => setShowImage({ ...showImage, ...{ [file.file.layer + ' buffers']: v } })} />{' '}
            </React.Fragment>)}
            <div style={{ position: 'relative' }}>
                {imageList}
            </div>
        </React.Fragment>}
    </React.Fragment>
}

interface LaserCalibrationProcess {
    v1: number
    v2: number
}

function LaserCalibration({ laserCalibration }: { laserCalibration: LaserCalibrationProcess }) {
    return <div>
        v1: {laserCalibration.v1} v2: {laserCalibration.v2} <br/>
    <Button onClick={()=>post("process/laserCalibration/printPattern").send()}>Print Pattern</Button>
     </div>;
}

interface Process {
    printPcb: PrintPcbProcess;
    laserCalibration: LaserCalibrationProcess
}

export default function ProcessComponent() {

    return <React.Fragment>
        <h1> Process</h1>
        <WithData<Process> url="process"
            refreshMs={1000}
            render={(process) => {
                let { printPcb, laserCalibration } = process;
                if (printPcb !== null) return <PrintPcb printPcb={printPcb} />;
                if (laserCalibration !== null) return <LaserCalibration laserCalibration={laserCalibration} />;

                return null;
            }} />
    </React.Fragment>;
}
