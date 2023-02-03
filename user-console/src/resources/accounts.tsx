import * as React from 'react';
import { useState } from 'react';
import {
    useResourceContext,
    useGetResourceLabel,
    Button,
    useTranslate,
    Edit,
    EditButton,
    useEditContext,
    useDataProvider,
    useGetIdentity,
    SimpleList,
    FunctionToElement,
    RaRecord,
    DateField,
    Labeled,
    TextField,
    Toolbar,
    DeleteWithConfirmButton,
    LinearProgress,
    RecordContextProvider,
    ListButton,
    SaveButton,
} from 'react-admin';
import { List, SimpleForm, TextInput } from 'react-admin';
import {
    Box,
    Grid,
    Typography,
    Card,
    ListItem,
    TableContainer,
    Paper,
    Table,
    TableBody,
    TableRow,
    TableCell,
    Avatar,
} from '@mui/material';
import Dialog from '@mui/material/Dialog';
import PersonIcon from '@mui/icons-material/Person';
import KeyIcon from '@mui/icons-material/Key';
import SwitchAccountIcon from '@mui/icons-material/SwitchAccount';
import GroupIcon from '@mui/icons-material/Group';
import GoogleIcon from '@mui/icons-material/Google';
import FacebookIcon from '@mui/icons-material/Facebook';
import AppleIcon from '@mui/icons-material/Apple';
import DeleteForeverIcon from '@mui/icons-material/DeleteForever';
import AlertError from '@mui/icons-material/ErrorOutline';

import { List as MuiList } from '@mui/material';

import GridList from '../components/gridList';
import { PageTitle } from '../components/pageTitle';

const getIcon = (record: any) => {
    if (record && record.authority === 'password') {
        return <KeyIcon fontSize="large" />;
    }
    if (record && record.authority === 'apple') {
        return <AppleIcon fontSize="large" />;
    }
    if (record && record.authority === 'oidc') {
        if (record.provider.toLowerCase() == 'google') {
            return <GoogleIcon fontSize="large" />;
        }
        if (record.provider.toLowerCase() == 'facebook') {
            return <FacebookIcon fontSize="large" />;
        }

        return <SwitchAccountIcon fontSize="large" />;
    }

    return <PersonIcon fontSize="large" />;
};

const UserActions = ({ user }: { user: any }) => {
    const translate = useTranslate();
    let userId = user.subjectId;

    return (
        // <RecordContextProvider record={data}>
        <Card sx={{ width: 1, p: 2, mr: 100 }}>
            <Typography variant="h6" sx={{ fontWeight: 600 }}>
                {translate('accounts_page.delete_user.title')}
            </Typography>
            <Typography sx={{ mb: 2 }}>
                {translate('accounts_page.delete_user.text')}
            </Typography>
            <Toolbar variant="dense" sx={{ width: 1 }}>
                <DeleteWithConfirmButton
                    record={user}
                    resource={'details'}
                    label="accounts_page.delete_user.action"
                    confirmTitle="accounts_page.delete_user.confirm"
                    confirmContent="accounts_page.delete_user.content"
                    icon={<DeleteForeverIcon />}
                    translateOptions={{ id: user.username }}
                />
            </Toolbar>
        </Card>
        // </RecordContextProvider>
    );
};

const UserProfile = ({ user }: { user: any }) => {
    const translate = useTranslate();

    return (
        <Card sx={{ width: 1, p: 2, mr: 100 }}>
            <Typography variant="h6" sx={{ fontWeight: 600 }}>
                {translate('accounts_page.registered_user')}
            </Typography>
            <MuiList>
                <ListItem>
                    <Labeled>
                        <TextField
                            label="username"
                            source="username"
                            record={user}
                        />
                    </Labeled>
                </ListItem>

                <ListItem>
                    <Labeled>
                        <TextField label="id" source="id" record={user} />
                    </Labeled>
                </ListItem>
            </MuiList>
        </Card>
    );
};

export const AccountList = () => {
    const translate = useTranslate();
    const { isLoading, data, error } = useGetIdentity();

    if (isLoading || !data) {
        return <LinearProgress />;
    }

    console.log('data', data);
    return (
        <Box component="div">
            <PageTitle
                text={translate('accounts_page.header')}
                secondaryText={translate('accounts_page.description')}
                icon={
                    <Avatar
                        sx={{
                            width: 72,
                            height: 72,
                            mb: 2,
                            alignItems: 'center',
                            display: 'inline-block',
                            textTransform: 'uppercase',
                            lineHeight: '102px',
                            backgroundColor: '#0066cc',
                        }}
                    >
                        <GroupIcon sx={{ fontSize: 48 }} />
                    </Avatar>
                }
            />
            <Grid container spacing={2}>
                <Grid item md={6} zeroMinWidth>
                    <UserProfile user={data} />
                </Grid>
                <Grid item md={6} zeroMinWidth>
                    <UserActions user={data} />
                </Grid>
            </Grid>
            <Typography variant="h5" sx={{ mt: 8, mb: 3 }}>
                {translate('accounts_page.header')}
            </Typography>
            <List component="div" pagination={false} actions={false}>
                <GridList
                    // key={record => record.username}
                    cols={6}
                    primaryText={record => {
                        return record.username;
                    }}
                    tertiaryText={record => {
                        return record.authority;
                    }}
                    icon={record => getIcon(record)}
                    secondaryText={record => (
                        <Box>
                            <MuiList>
                                <ListItem>
                                    <Labeled>
                                        <TextField label="id" source="uuid" />
                                    </Labeled>
                                </ListItem>
                                {record.email && (
                                    <ListItem>
                                        <Labeled>
                                            <TextField
                                                label="emailAddress"
                                                source="email"
                                            />
                                        </Labeled>
                                    </ListItem>
                                )}

                                <ListItem>
                                    <Labeled>
                                        <DateField
                                            label="created"
                                            source="createDate"
                                        />
                                    </Labeled>
                                </ListItem>
                                <ListItem>
                                    <Labeled>
                                        <DateField
                                            label="modified"
                                            source="modifiedDate"
                                        />
                                    </Labeled>
                                </ListItem>
                            </MuiList>
                        </Box>
                    )}
                    actions={record => {
                        return (
                            <Toolbar variant="dense" sx={{ width: 1 }}>
                                {record.authority === 'internal' && (
                                    <EditButton />
                                )}
                                <DeleteWithConfirmButton
                                    confirmContent="accounts_page.delete_account.content"
                                    translateOptions={{ id: record.username }}
                                />
                            </Toolbar>
                        );
                    }}
                />
            </List>
        </Box>
    );
};

export const AccountEdit = () => {
    return (
        <Edit mutationMode="pessimistic">
            <AccountEditForm />
        </Edit>
    );
};
export const AccountEditForm = () => {
    const { record, isLoading } = useEditContext();
    if (isLoading || !record) {
        return <LinearProgress />;
    }

    if (record.authority == 'internal') {
        return <InternalAccountEditForm />;
    }

    return <div></div>;
};
export const InternalAccountEditForm = () => {
    return (
        <SimpleForm
            toolbar={
                <Toolbar>
                    <SaveButton />
                    <ListButton
                        icon={<AlertError />}
                        label="ra.action.cancel"
                    />
                </Toolbar>
            }
        >
            <TextInput source="name" />
            <TextInput source="surname" />
            <TextInput source="email" />
            {/* <TextInput source="lang" /> */}
        </SimpleForm>
    );
};
