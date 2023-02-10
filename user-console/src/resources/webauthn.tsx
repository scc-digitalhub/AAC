import {
    useTranslate,
    EditButton,
    DateField,
    Labeled,
    TextField,
    Toolbar,
    Edit,
    DeleteWithConfirmButton,
    ListButton,
    SaveButton,
    useGetList,
    Create,
    useDataProvider,
    useNotify,
    useCreateContext,
    SaveContextProvider,
} from 'react-admin';
import { List, SimpleForm, TextInput } from 'react-admin';
import { Box, Typography, Alert } from '@mui/material';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import AlertError from '@mui/icons-material/ErrorOutline';

import { Stack } from '@mui/material';

import GridList from '../components/gridList';
import { CardToolbar } from '../components/cardToolbar';
import CreateButton from '../components/createButton';

import {
    startRegistration,
    browserSupportsWebAuthn,
} from '@simplewebauthn/browser';
import { RegistrationResponseJSON } from '@simplewebauthn/typescript-types';

export const WebAuthnList = () => {
    return (
        <List
            resource="password"
            component="div"
            pagination={false}
            actions={false}
        >
            <WebAuthnGridList />
        </List>
    );
};

export const WebAuthnGridList = () => {
    const translate = useTranslate();

    return (
        <GridList
            // key={record => record.username}
            cols={6}
            primaryText={record => {
                return record.displayName;
            }}
            tertiaryText={record => {
                return record.id;
            }}
            icon={<AdminPanelSettingsIcon />}
            secondaryText={record => (
                <Box>
                    <Typography variant="subtitle1" sx={{ mb: 2 }}>
                        {translate('resources.webauthn.details')}
                    </Typography>
                    <Stack direction="row" spacing={2}>
                        <Labeled>
                            <TextField label="username" source="username" />
                        </Labeled>

                        <Labeled>
                            <DateField label="createDate" source="createDate" />
                        </Labeled>
                        <Labeled>
                            <DateField
                                label="lastUsedDate"
                                source="lastUsedDate"
                                showTime
                            />
                        </Labeled>
                    </Stack>
                </Box>
            )}
            actions={record => {
                return (
                    <CardToolbar variant="dense" sx={{ width: 1 }}>
                        <EditButton />
                        <DeleteWithConfirmButton
                            confirmContent="page.webauthn.delete.content"
                            label="ra.action.remove"
                            translateOptions={{
                                id: record.displayName,
                            }}
                        />
                    </CardToolbar>
                );
            }}
        />
    );
};

export const WebAuthnAddToolbar = () => {
    const translate = useTranslate();
    const { data, isLoading } = useGetList('accounts');

    if (isLoading || !data) {
        return <div></div>;
    }

    //we need support from browser
    if (!browserSupportsWebAuthn()) {
        return (
            <Alert severity="warning">
                {translate('alert.webauthn_unsupported')}
            </Alert>
        );
    }

    //to add we need an internal account
    const account = data.find(a => a.authority === 'internal');
    if (!account) {
        return (
            <Alert severity="info">{translate('alert.missing_account')}</Alert>
        );
    }

    return (
        <CardToolbar>
            <CreateButton label="action.register" />
        </CardToolbar>
    );
};

export const WebAuthnEdit = () => {
    return (
        <Edit>
            <WebAuthnEditForm />
        </Edit>
    );
};

export const WebAuthnCreate = () => {
    return (
        <Create redirect="list">
            <WebAuthnCreateForm />
        </Create>
    );
};

export const WebAuthnCreateForm = () => {
    const translate = useTranslate();
    const dataProvider = useDataProvider();
    const notify = useNotify();
    const { save } = useCreateContext();

    let registering = false;
    const mutationMode = 'pessimistic';
    const register = function (values: any) {
        registering = true;

        const data = {
            displayName: values ? values.displayName : '',
            authority: 'webauthn',
            type: 'credentials:webauthn',
            key: '',
            attestation: '',
        };

        //step1
        const req = {
            path: 'webauthn',
            options: { method: 'PATCH' },
            body: JSON.stringify(data),
        };

        dataProvider
            .invoke(req)
            .then(function (json: any) {
                if (!json.key || !json.options || !json.options.publicKey) {
                    throw new Error('alert.invalid_attestation');
                }

                //save key
                data.key = json.key;

                //step2
                return startRegistration(json.options.publicKey);
            })
            .then(function (reg: RegistrationResponseJSON) {
                //save attestation
                data.attestation = JSON.stringify(reg);
                return data;
            })
            .then(function (record: any) {
                if (save) {
                    save(record);
                    registering = false;
                }
            })
            .catch(function (error: any) {
                notify(error.toString(), { type: 'error' });
                registering = false;
            });
    };

    return (
        <SaveContextProvider
            value={{ save: register, saving: registering, mutationMode }}
        >
            <SimpleForm
                toolbar={
                    <Toolbar>
                        <SaveButton
                            type="button"
                            label="action.register"
                            // transform={handle}
                        />
                        <ListButton
                            icon={<AlertError />}
                            label="ra.action.cancel"
                        />
                    </Toolbar>
                }
            >
                <Typography variant="h5">
                    {translate('page.webauthn.create.title')}
                </Typography>
                <Typography variant="subtitle1" sx={{ mb: 2 }}>
                    {translate('page.webauthn.create.description')}
                </Typography>
                <TextInput source="displayName" />
            </SimpleForm>
        </SaveContextProvider>
    );
};

export const WebAuthnEditForm = () => {
    const translate = useTranslate();

    return (
        <SimpleForm
            toolbar={
                <Toolbar>
                    <SaveButton label="ra.action.update" />
                    <ListButton
                        icon={<AlertError />}
                        label="ra.action.cancel"
                    />
                </Toolbar>
            }
        >
            <Typography variant="h5">
                {translate('page.webauthn.edit.title')}
            </Typography>
            <Typography variant="subtitle1" sx={{ mb: 2 }}>
                {translate('page.webauthn.edit.description')}
            </Typography>
            <TextInput source="displayName" />
        </SimpleForm>
    );
};
