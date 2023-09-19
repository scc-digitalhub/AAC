import {
    Edit,
    EditButton,
    useEditContext,
    DateField,
    Labeled,
    TextField,
    Toolbar,
    DeleteWithConfirmButton,
    LinearProgress,
    ListButton,
    SaveButton,
    useTranslate,
} from 'react-admin';
import { List, SimpleForm, TextInput } from 'react-admin';
import { Box, ListItem, Typography } from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';
import KeyIcon from '@mui/icons-material/Key';
import SwitchAccountIcon from '@mui/icons-material/SwitchAccount';
import GoogleIcon from '@mui/icons-material/Google';
import FacebookIcon from '@mui/icons-material/Facebook';
import AppleIcon from '@mui/icons-material/Apple';
import AlertError from '@mui/icons-material/ErrorOutline';

import { List as MuiList } from '@mui/material';

import GridList from '../components/gridList';
import { CardToolbar } from '../components/cardToolbar';

const getIcon = (record: any) => {
    if (record && record.authority === 'password') {
        return <KeyIcon fontSize="large" />;
    }
    if (record && record.authority === 'apple') {
        return <AppleIcon fontSize="large" />;
    }
    if (record && record.authority === 'oidc') {
        if (record.provider.toLowerCase() === 'google') {
            return <GoogleIcon fontSize="large" />;
        }
        if (record.provider.toLowerCase() === 'facebook') {
            return <FacebookIcon fontSize="large" />;
        }

        return <SwitchAccountIcon fontSize="large" />;
    }

    return <PersonIcon fontSize="large" />;
};

export const AccountsList = () => {
    return (
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
                        <CardToolbar variant="dense" sx={{ width: 1 }}>
                            {record.authority === 'internal' && <EditButton />}
                            <DeleteWithConfirmButton
                                confirmContent="page.accounts.delete_account.content"
                                translateOptions={{ id: record.username }}
                            />
                        </CardToolbar>
                    );
                }}
            />
        </List>
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

    if (record.authority === 'internal') {
        return <InternalAccountEditForm />;
    }

    return <div></div>;
};
export const InternalAccountEditForm = () => {
    const translate = useTranslate();

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
            <Typography variant="h5">
                {translate('page.accounts.edit.title')}
            </Typography>
            <Typography variant="subtitle1" sx={{ mb: 2 }}>
                {translate('page.accounts.edit.description')}
            </Typography>
            <TextInput source="name" />
            <TextInput source="surname" />
            <TextInput source="email" />
            {/* <TextInput source="lang" /> */}
        </SimpleForm>
    );
};
