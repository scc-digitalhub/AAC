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
    ListContextProvider,
    useGetList,
    useList,
    PasswordInput,
    required,
    useInput,
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
    ListItemButton,
    ListItemIcon,
    ListItemText,
} from '@mui/material';
import Dialog from '@mui/material/Dialog';
import PersonIcon from '@mui/icons-material/Person';
import KeyIcon from '@mui/icons-material/Key';
import SwitchAccountIcon from '@mui/icons-material/SwitchAccount';
import GroupIcon from '@mui/icons-material/Group';
import GoogleIcon from '@mui/icons-material/Google';
import FacebookIcon from '@mui/icons-material/Facebook';
import AppleIcon from '@mui/icons-material/Apple';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import AlertError from '@mui/icons-material/ErrorOutline';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import DoneOutlineIcon from '@mui/icons-material/DoneOutline';
import KeyboardArrowRightIcon from '@mui/icons-material/KeyboardArrowRight';
import CheckBoxOutlineBlankIcon from '@mui/icons-material/CheckBoxOutlineBlank';
import DoneIcon from '@mui/icons-material/Done';
import ErrorIcon from '@mui/icons-material/Error';

import { List as MuiList, Stack } from '@mui/material';

import GridList from '../components/gridList';
import { PageTitle } from '../components/pageTitle';
import { Spacer } from '../components/spacer';
import { FunctionExpression } from 'typescript';
import PasswordStrengthBar from 'react-password-strength-bar';

const getIcon = (record: any) => {
    if (record && record.authority === 'password') {
        return <KeyIcon fontSize="large" />;
    }
    if (record && record.authority === 'webauthn') {
        return <AdminPanelSettingsIcon fontSize="large" />;
    }

    return <VpnKeyIcon fontSize="large" />;
};

export const CredentialsList = () => {
    const translate = useTranslate();
    const { data, isLoading } = useGetList('credentials');

    return (
        <Box component="div">
            <PageTitle
                text={translate('credentials_page.header')}
                secondaryText={translate('credentials_page.description')}
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
                        <VpnKeyIcon sx={{ fontSize: 48 }} />
                    </Avatar>
                }
            />

            <Typography variant="h5" sx={{ mb: 2 }}>
                {translate('credentials_page.password.title')}
            </Typography>
            <PasswordList credentials={data} isLoading={isLoading} />
            <Spacer space="3rem" />
            <Typography variant="h5" sx={{ mb: 2 }}>
                {translate('credentials_page.webauthn.title')}
            </Typography>
            <WebAuthnList credentials={data} isLoading={isLoading} />
        </Box>
    );
};

interface CredentialsListProp<RecordType extends RaRecord = any> {
    credentials?: RecordType[];
    isLoading?: boolean;
}

const PasswordList = (props: CredentialsListProp) => {
    const translate = useTranslate();

    const { credentials, isLoading } = props;
    let data = [];
    if (credentials) {
        data = credentials.filter(c => c.authority === 'password');
    }

    const listContext = useList({ data, isLoading });

    if (!isLoading && (!data || data.length === 0)) {
        return <div>AddPassword</div>;
    }

    return (
        <ListContextProvider value={listContext}>
            <Typography variant="subtitle1" sx={{ mb: 2 }}>
                {translate('credentials_page.webauthn.subtitle')}
            </Typography>
            {data && data.length === 0 && <div>setPassword</div>}

            {data && data.length > 0 && (
                <GridList
                    // key={record => record.username}
                    cols={12}
                    primaryText="password"
                    tertiaryText={record => {
                        return record.id;
                    }}
                    icon={record => getIcon(record)}
                    secondaryText={record => (
                        <Box>
                            <Typography variant="subtitle1" sx={{ mb: 2 }}>
                                {translate('credentials_page.password.details')}
                            </Typography>
                            <Stack direction="row" spacing={2}>
                                <Labeled>
                                    <TextField
                                        label="username"
                                        source="username"
                                    />
                                </Labeled>

                                <Labeled>
                                    <DateField
                                        label="modifiedDate"
                                        source="modifiedDate"
                                    />
                                </Labeled>
                            </Stack>
                        </Box>
                    )}
                    actions={record => {
                        return (
                            <Toolbar variant="dense" sx={{ width: 1 }}>
                                <EditButton />
                            </Toolbar>
                        );
                    }}
                />
            )}
        </ListContextProvider>
    );
};

const WebAuthnList = (props: CredentialsListProp) => {
    const translate = useTranslate();

    const { credentials, isLoading } = props;
    let data = [];
    if (credentials) {
        data = credentials.filter(c => c.authority === 'webauthn');
    }

    const listContext = useList({ data, isLoading });

    return (
        <ListContextProvider value={listContext}>
            <Typography variant="subtitle1" sx={{ mb: 2 }}>
                {translate('credentials_page.webauthn.subtitle')}
            </Typography>
            <GridList
                // key={record => record.username}
                cols={6}
                primaryText={record => {
                    return record.displayName;
                }}
                tertiaryText={record => {
                    return record.id;
                }}
                icon={record => getIcon(record)}
                secondaryText={record => (
                    <Box>
                        <Typography variant="subtitle1" sx={{ mb: 2 }}>
                            {translate('credentials_page.webauthn.details')}
                        </Typography>
                        <Stack direction="row" spacing={2}>
                            <Labeled>
                                <TextField label="username" source="username" />
                            </Labeled>

                            <Labeled>
                                <DateField
                                    label="createDate"
                                    source="createDate"
                                />
                            </Labeled>
                            <Labeled>
                                <DateField
                                    label="lastUsedDate"
                                    source="lastUsedDate"
                                />
                            </Labeled>
                        </Stack>
                    </Box>
                )}
                actions={record => {
                    return (
                        <Toolbar variant="dense" sx={{ width: 1 }}>
                            <EditButton />
                            <DeleteWithConfirmButton
                                confirmContent="credentials_page.delete_credential.content"
                                label="ra.action.remove"
                                translateOptions={{
                                    id: record.displayName,
                                }}
                            />
                        </Toolbar>
                    );
                }}
            />
        </ListContextProvider>
    );
};

// const WebAuthnList = (props: CredentialsListProp) => {
//     const { credentials, isLoading } = props;
//     return (
//         <div></div>
//         <List component="div" pagination={false} actions={false}>
//             <GridList
//                 // key={record => record.username}
//                 cols={6}
//                 primaryText={record => {
//                     return record.id;
//                 }}
//                 tertiaryText={record => {
//                     return record.authority;
//                 }}
//                 icon={record => getIcon(record)}
//                 secondaryText={record => <Box></Box>}
//                 actions={record => {
//                     return (
//                         <Toolbar variant="dense" sx={{ width: 1 }}>
//                             <EditButton />
//                             {record && record.authority === 'webauthn' && (
//                                 <DeleteWithConfirmButton
//                                     confirmContent="credentials_page.delete_credential.content"
//                                     translateOptions={{
//                                         id: record.displayName,
//                                     }}
//                                 />
//                             )}
//                         </Toolbar>
//                     );
//                 }}
//             />
//         </List>
//     );
// };

export const CredentialEdit = () => {
    return (
        <Edit mutationMode="pessimistic">
            <CredentialEditForm />
        </Edit>
    );
};
const CredentialEditForm = () => {
    const { record, isLoading } = useEditContext();
    if (isLoading || !record) {
        return <LinearProgress />;
    }

    if (record.authority === 'password') {
        return <PasswordEditForm />;
    }
    if (record.authority === 'webauthn') {
        return <WebAuthnEditForm />;
    }
    return <div></div>;
};

const PasswordEditForm = () => {
    const translate = useTranslate();

    const validate = (values: any) => {
        const errors: { [key: string]: any } = {};

        //curPassword is set
        if (!values.curPassword) {
            errors.curPassword = 'error.invalid_password.empty';
        }

        //password is set
        if (!values.password) {
            errors.password = 'error.invalid_password.empty';
        }

        //password matches regex
        if (values.password && values.policy && values.policy.passwordPattern) {
            if (!values.password.match(values.policy.passwordPattern)) {
                errors.password = 'error.invalid_password.policy';
            }
        }

        //verifyPassword is set
        if (!values.verifyPassword) {
            errors.verifyPassword = 'error.invalid_password.empty';
        }

        //verifyPassword matches password
        if (values.password !== values.verifyPassword) {
            errors.password = 'error.invalid_password.not_match';
            errors.verifyPassword = 'error.invalid_password.not_match';
        }

        return errors;
    };

    return (
        <SimpleForm
            validate={validate}
            criteriaMode="all"
            mode="onBlur"
            reValidateMode="onBlur"
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
                {translate('credentials_page.password.edit.title')}
            </Typography>
            <Typography variant="subtitle1" sx={{ mb: 2 }}>
                {translate('credentials_page.password.edit.description')}
            </Typography>
            <PasswordInput
                source="curPassword"
                inputProps={{ autocomplete: 'current-password' }}
                validate={required()}
            />
            <PasswordInput source="password" validate={required()} />
            <PasswordInput source="verifyPassword" validate={required()} />
            <PasswordPolicyInput />
        </SimpleForm>
    );
};

const PasswordPolicyInput = () => {
    const translate = useTranslate();

    const { record, isLoading } = useEditContext();
    const { field } = useInput({ source: 'password' });

    if (isLoading || !record) {
        return <div></div>;
    }

    return (
        <Box>
            <Typography variant="subtitle1">
                {translate('password.policy.strength')}
            </Typography>
            <PasswordStrengthBar password={field.value} />
            <Spacer />
            {record.policy && (
                <PasswordPolicy policy={record.policy} password={field.value} />
            )}
        </Box>
    );
};

export const PasswordPolicy = ({
    policy,
    password,
}: {
    policy: any;
    password?: string;
}) => {
    const translate = useTranslate();

    const getIcon = (status?: boolean) => {
        if (status === true) {
            return <DoneIcon />;
        }
        if (status === false) {
            return <ErrorIcon />;
        }
        return <CheckBoxOutlineBlankIcon />;
    };

    const getColor = (status?: boolean) => {
        if (status === true) {
            return 'success.main';
        }
        if (status === false) {
            return 'error.main';
        }
        return 'text.primary';
    };

    const passwordMinLength = (value: any) => {
        if (password) {
            return password.length >= value;
        }
        return undefined;
    };
    const passwordMaxLength = (value: any) => {
        if (password) {
            return password.length <= value;
        }
        return undefined;
    };

    const passwordRequireAlpha = (value: any) => {
        if (password && value) {
            return password.match(/[A-Za-z]/) ? true : false;
        }
        return undefined;
    };
    const passwordRequireUppercaseAlpha = (value: any) => {
        if (password && value) {
            return password.match(/[A-Z]/) ? true : false;
        }
        return undefined;
    };
    const passwordRequireNumber = (value: any) => {
        if (password && value) {
            return password.match(/\d/) ? true : false;
        }
        return undefined;
    };
    const passwordRequireSpecial = (value: any) => {
        if (password && value) {
            return password.match(/[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?~]/)
                ? true
                : false;
        }
        return undefined;
    };
    const passwordSupportWhitespace = (value: any) => {
        if (password && value === false) {
            return password.match(/\s/) ? false : true;
        }
        if (password && value === true) {
            return true;
        }
        return undefined;
    };
    const validate = (criteria: string, value: any) => {
        if (criteria === 'passwordMinLength') {
            return passwordMinLength(value);
        }
        if (criteria === 'passwordMaxLength') {
            return passwordMaxLength(value);
        }
        if (criteria === 'passwordRequireAlpha') {
            return passwordRequireAlpha(value);
        }
        if (criteria === 'passwordRequireUppercaseAlpha') {
            return passwordRequireUppercaseAlpha(value);
        }
        if (criteria === 'passwordRequireNumber') {
            return passwordRequireNumber(value);
        }
        if (criteria === 'passwordRequireSpecial') {
            return passwordRequireSpecial(value);
        }
        if (criteria === 'passwordSupportWhitespace') {
            return passwordSupportWhitespace(value);
        }
        return undefined;
    };

    return (
        <Box>
            <Typography variant="subtitle1">
                {translate('password.policy.description')}
            </Typography>
            <MuiList>
                {/* {policy.passwordMinLength && (
                    <ListItem disableGutters disablePadding>
                        <ListItemIcon
                            sx={{
                                minWidth: 32,
                                color: getColor(passwordMinLength()),
                            }}
                        >
                            {getIcon(passwordMinLength())}
                        </ListItemIcon>
                        <ListItemText
                            sx={{ color: getColor(passwordMinLength()) }}
                            primary={translate(
                                'password.policy.passwordMinLength',
                                {
                                    value: policy.passwordMinLength,
                                }
                            )}
                        />
                    </ListItem>
                )} */}

                {Object.entries(policy)
                    .filter(c => c[0] !== 'passwordPattern')
                    .filter(c => c[1] !== false)
                    .map((p: any, key: any) => {
                        const criteria = p[0];
                        const value = p[1];
                        const valid = validate(criteria, value);
                        return (
                            <ListItem disableGutters disablePadding>
                                <ListItemIcon
                                    sx={{
                                        minWidth: 32,
                                        color: getColor(valid),
                                    }}
                                >
                                    {getIcon(valid)}
                                </ListItemIcon>
                                <ListItemText
                                    sx={{
                                        color: getColor(valid),
                                    }}
                                    primary={translate(
                                        'password.policy.' + criteria,
                                        {
                                            value: value,
                                        }
                                    )}
                                />
                            </ListItem>
                        );
                    })}
            </MuiList>
        </Box>
    );
};

const WebAuthnEditForm = () => {
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
                {translate('credentials_page.webauthn.edit.title')}
            </Typography>
            <Typography variant="subtitle1" sx={{ mb: 2 }}>
                {translate('credentials_page.webauthn.edit.description')}
            </Typography>
            <TextInput source="displayName" />
        </SimpleForm>
    );
};
