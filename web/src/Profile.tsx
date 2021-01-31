import WithData from './WithData'
import React, { useState, useEffect } from 'react';
import { request, post, useRefreshTrigger } from './useData';
import { toast } from 'react-toastify';
import Select from 'react-dropdown-select';
import { Row, Col, Button } from 'react-bootstrap';

interface EditFunctions<T> {
    update(value: Partial<T>): void;
    save(onSuccess?: () => void): void;
}
function useEdit<T>(url?: string): [T | undefined, EditFunctions<T>] {
    const [state, setState] = useState<{ status: 'loading' } | { status: 'error', error: string } | { status: 'loaded', value?: T }>({ status: 'loading' });
    useEffect(() => {
        if (url === undefined) {
            setState({ status: 'loaded' });
            return;
        }
        request(url).method('GET').success(value => setState({ status: 'loaded', value })).error(error => setState({ status: 'error', error })).send();
    }, [url]);

    return [state.status === 'loaded' ? state.value : undefined, {
        update: (value) => state.status === 'loaded' ? setState({ status: 'loaded', value: state.value === undefined ? undefined : { ...(state.value), ...value } }) : null,
        save: (onSuccess?: () => void) => {
            if (url === undefined) {
                toast.error('url undefined');
                return;
            }
            if (url === undefined) {
                toast.error('url undefined');
                return;
            }
            if (state.status === 'loaded' && state.value !== undefined && url !== undefined)
                post(url).body(state.value).success(() => {
                    toast.success("Saved");
                    if (onSuccess !== undefined)
                        onSuccess();
                }).error('Error during save').send();
        }
    }];
}

interface Profile {
    id: string;
    name: string;
}

export default function ProfileComponent() {
    const [selectedProfile, setSelectedProfile] = useState<Profile[]>([]);
    const [profile, editProfile] = useEdit<Profile>(selectedProfile.length === 0 ? undefined : 'profile/' + selectedProfile[0].id);
    const profilesRefreshTrigger = useRefreshTrigger();
    useEffect(() => request('profile/current').success(profile => {if (profile !== null) setSelectedProfile([profile])}).send(), []);
    return <React.Fragment>
        <h1> Profile </h1>
        <div className="container-fluid">

            <WithData<Profile[]> url="profile" trigger={profilesRefreshTrigger} render={(data, trigger) =>
                <Row>
                    <Col>
                        <Select options={data} values={selectedProfile} onChange={value=>{setSelectedProfile(value); 
                        if (value.length>0) post('profile/current').query({id:value[0].id}).send();
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
            </React.Fragment>}
        </div>
    </React.Fragment>
}
