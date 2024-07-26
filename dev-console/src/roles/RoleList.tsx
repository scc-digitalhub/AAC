import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import {
    List,
    Datagrid,
    TextField,
    TopToolbar,
    CreateButton,
    EditButton,
    ShowButton,
    useTranslate,
    SearchInput,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import { RoleCreateForm } from './RoleCreate';
import { Box, Typography } from '@mui/material';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { DropDownButton } from '../components/DropdownButton';
import { IdField } from '../components/IdField';
import { RowButtonGroup } from '../components/RowButtonGroup';
import { EnableIdpButton } from '../idps/IdpList';
import { ActionsButtons } from '../components/ActionsButtons';
import { PageTitle } from '../components/pageTitle';
import { YamlExporter } from '../components/YamlExporter';

export const RoleList = () => {
    const translate = useTranslate();
    return (
        <>
        <PageTitle
            text={translate('page.group.list.title')}
            secondaryText={translate('page.group.list.subtitle')}
        />
        <List
            exporter={YamlExporter}
            actions={<RoleListActions />}
            filters={RoleFilters}
            sort={{ field: 'name', order: 'DESC' }}
        >
            <Datagrid bulkActionButtons={false} rowClick="show">
            <TextField source="name" />
            <IdField source="id" />
            <TextField source="authority" />
                <ActionsButtons />
            </Datagrid>
        </List>
        </>
    );
};
const RoleFilters = [<SearchInput source="q" alwaysOn />]; 
const Empty = () => {
    return (
        <Box textAlign="center" mt={30} ml={70}>
            <Typography variant="h6" paragraph>
                No Role available, create one
            </Typography>
            <CreateInDialogButton
                fullWidth
                maxWidth={'md'}
                variant="contained"
                transform={createTransform}
            >
                <RoleCreateForm />
            </CreateInDialogButton>
        </Box>
    );
};
const createTransform = (data: any) => {
    return {
        ...data,
    };
};
const RoleListActions = () => {
    return (
        <TopToolbar>
            <CreateInDialogButton
                fullWidth
                maxWidth={'md'}
                variant="contained"
                transform={createTransform}
            >
                <RoleCreateForm />
            </CreateInDialogButton>
        </TopToolbar>
    );
};
