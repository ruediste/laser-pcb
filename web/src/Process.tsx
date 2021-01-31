import React, { useState } from 'react';
import { Button, Form } from 'react-bootstrap';
import Select from 'react-dropdown-select';
import { post, request, baseUrl } from './useData';
import { toast } from 'react-toastify';
import WithData from './WithData';
import { ChevronDownIcon, ChevronRightIcon } from '@primer/octicons-react';

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
            <NoClickBubble><Select options={[{ label: 'TOP', value: 'TOP' }, { label: 'BOTTOM', value: "BOTTOM" }]} values={file.file.layer === undefined ? [] : [{ label: file.file.layer, value: file.file.layer }]} onChange={(value) => { }} /></NoClickBubble>
            <NoClickBubble><Button onClick={(e) => request('process/file/' + file.file.id).method('DELETE').success('File ' + file.file.name + " removed").send()}>Remove</Button></NoClickBubble>
        </div>
        {
            open ? <div>
                {file.file.errorMessage !== undefined ? file.file.errorMessage : null}
                {file.svgAvailable ? <img src={baseUrl + 'process/file/svg/' + file.file.id + '.svg'} style={{ width: '50%' }} /> : null}
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
                    post("process/_addFile").bodyRaw(file).query({ name: file.name })
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
        status: 'PARSING' | 'PARSED' | 'ERROR';
        layer?: string;
        id: string;
    }
    svgAvailable: boolean;
}

interface PrintPcbProcess {
    inputFiles: PrintPcbInputFile[];
}

interface Process {
    printPcb: PrintPcbProcess;
}
export default function ProcessComponent() {
    return <React.Fragment>
        <h1> Process</h1>
        <WithData<Process> url="process"
            refreshMs={1000}
            render={(process) => {
                let { printPcb } = process;
                if (printPcb !== undefined)
                    return <React.Fragment>
                        <h1> Gerber Files</h1>
                        <GerberFiles uploadedFiles={printPcb.inputFiles} />
                    </React.Fragment>

                return null;
            }} />
    </React.Fragment>;
}
