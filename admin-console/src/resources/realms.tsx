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
    Datagrid,
    BooleanField,
    BooleanInput,
    Button,
    CreateButton,
    ExportButton,
    FilterButton,
    FilterForm,
    TopToolbar,
    Create,
    regex,
} from 'react-admin';
import { List, SimpleForm, TextInput } from 'react-admin';
import { Box, ListItem, Stack, Typography } from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';
import KeyIcon from '@mui/icons-material/Key';
import SwitchAccountIcon from '@mui/icons-material/SwitchAccount';
import GoogleIcon from '@mui/icons-material/Google';
import FacebookIcon from '@mui/icons-material/Facebook';
import AppleIcon from '@mui/icons-material/Apple';
import AlertError from '@mui/icons-material/ErrorOutline';
import { Toolbar as MuiToolbar } from '@mui/material';

import { List as MuiList } from '@mui/material';

import GridList from '../components/gridList';
import { CardToolbar } from '../components/cardToolbar';
import SelectButton from '../components/SelectButton';

const ListActions = () => (
    <TopToolbar>
        <CreateButton />
    </TopToolbar>
);

export const RealmsList = () => {
    const filters = [<TextInput label="Search" source="q" alwaysOn />];

    return (
        <List actions={<ListActions />} filters={filters}>
            <Datagrid bulkActionButtons={false} size="medium">
                <TextField source="slug" />
                <TextField source="name" />
                <BooleanField source="public" />
                <MuiToolbar>
                    <SelectButton />
                    <EditButton />
                    <DeleteWithConfirmButton />
                </MuiToolbar>
            </Datagrid>
        </List>
    );
};

export const RealmCreate = () => {
    const translate = useTranslate();
    const validateSlug = regex(/^[a-zA-Z0-9_-]+$/, 'error.invalid_slug');
    return (
        <Create redirect="list">
            <SimpleForm
                toolbar={
                    <Toolbar>
                        <SaveButton label="ra.action.add" />
                        <ListButton
                            icon={<AlertError />}
                            label="ra.action.cancel"
                        />
                    </Toolbar>
                }
            >
                <Typography variant="h5">
                    {translate('page.realms.create.title')}
                </Typography>
                <Typography variant="subtitle1" sx={{ mb: 2 }}>
                    {translate('page.realms.create.description')}
                </Typography>
                <TextInput
                    source="slug"
                    validate={validateSlug}
                    required
                    helperText="resources.realms.helperText.slug"
                />
                <TextInput
                    source="name"
                    required
                    helperText="resources.realms.helperText.name"
                />
                <BooleanInput
                    source="public"
                    helperText="resources.realms.helperText.public"
                />
            </SimpleForm>
        </Create>
    );
};

export const RealmEdit = () => {
    const translate = useTranslate();
    return (
        <Edit mutationMode="pessimistic">
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
                    {translate('page.realms.edit.title')}
                </Typography>
                <Typography variant="subtitle1" sx={{ mb: 2 }}>
                    {translate('page.realms.edit.description')}
                </Typography>
                <TextInput source="name" />
                <BooleanInput source="public" />
            </SimpleForm>
        </Edit>
    );
};
