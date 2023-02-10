import {
    useTranslate,
    EditButton,
    useEditContext,
    DateField,
    Labeled,
    TextField,
    Toolbar,
    ListButton,
    SaveButton,
    useGetList,
    PasswordInput,
    required,
    useInput,
    DeleteWithConfirmButton,
    Edit,
    Create,
} from 'react-admin';
import { List, SimpleForm } from 'react-admin';
import {
    Box,
    Typography,
    ListItem,
    ListItemIcon,
    ListItemText,
    Alert,
} from '@mui/material';
import KeyIcon from '@mui/icons-material/Key';
import AlertError from '@mui/icons-material/ErrorOutline';
import CheckBoxOutlineBlankIcon from '@mui/icons-material/CheckBoxOutlineBlank';
import DoneIcon from '@mui/icons-material/Done';
import ErrorIcon from '@mui/icons-material/Error';

import { List as MuiList, Stack } from '@mui/material';

import GridList from '../components/gridList';
import { CardToolbar } from '../components/cardToolbar';
import { Spacer } from '../components/spacer';
import CreateButton from '../components/createButton';

import PasswordStrengthBar from 'react-password-strength-bar';

export const PasswordList = () => {
    return (
        <List
            resource="password"
            component="div"
            pagination={false}
            actions={false}
        >
            <PasswordGridList />
        </List>
    );
};

export const PasswordGridList = () => {
    const translate = useTranslate();

    return (
        <GridList
            // key={record => record.username}
            cols={12}
            primaryText="password"
            tertiaryText={record => {
                return record.id;
            }}
            icon={<KeyIcon />}
            secondaryText={record => (
                <Box>
                    <Typography variant="subtitle1" sx={{ mb: 2 }}>
                        {translate('resources.password.details')}
                    </Typography>
                    <Stack direction="row" spacing={2}>
                        <Labeled>
                            <TextField label="username" source="username" />
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
                    <CardToolbar variant="dense" sx={{ width: 1 }}>
                        <EditButton />
                        <DeleteWithConfirmButton
                            confirmContent="page.password.delete.content"
                            label="ra.action.remove"
                            translateOptions={{
                                id: record.username,
                            }}
                        />
                    </CardToolbar>
                );
            }}
        />
    );
};

export const PasswordAddToolbar = () => {
    const translate = useTranslate();
    const { data, isLoading } = useGetList('accounts');

    if (isLoading || !data) {
        return <div></div>;
    }

    //to add a password we need an internal account
    const account = data.find(a => a.authority === 'internal');
    if (!account) {
        return (
            <Alert severity="info">{translate('alert.missing_account')}</Alert>
        );
    }

    return (
        <CardToolbar>
            <CreateButton />
        </CardToolbar>
    );
};

export const PasswordEdit = () => {
    return (
        <Edit>
            <PasswordEditForm askCurrent={true} />
        </Edit>
    );
};

export const PasswordCreate = () => {
    return (
        <Create>
            <PasswordEditForm askCurrent={false} />
        </Create>
    );
};

export const PasswordEditForm = ({ askCurrent }: { askCurrent?: boolean }) => {
    const translate = useTranslate();

    const validate = (values: any) => {
        const errors: { [key: string]: any } = {};

        //curPassword is set
        if (askCurrent && !values.curPassword) {
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
                {translate('page.password.edit.title')}
            </Typography>
            <Typography variant="subtitle1" sx={{ mb: 2 }}>
                {translate('page.password.edit.description')}
            </Typography>
            {askCurrent && (
                <PasswordInput
                    source="curPassword"
                    inputProps={{ autoComplete: 'current-password' }}
                    validate={required()}
                />
            )}
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
                {translate('resources.password.policy.strength')}
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
                {translate('resources.password.policy.description')}
            </Typography>
            <MuiList>
                {Object.entries(policy)
                    .filter(c => c[0] !== 'passwordPattern')
                    .filter(c => c[1] !== false)
                    .map((p: any, key: any) => {
                        const criteria = p[0];
                        const value = p[1];
                        const valid = validate(criteria, value);
                        return (
                            <ListItem
                                key={'password.policy.' + criteria}
                                disableGutters
                                disablePadding
                            >
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
                                        'resources.password.policy.' + criteria,
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
