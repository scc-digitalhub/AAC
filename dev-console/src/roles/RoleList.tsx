import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import {
    List,
    Datagrid,
    TextField,
    TopToolbar,
    CreateButton,
    EditButton,
    ShowButton,
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

export const RoleList = () => {
    return (
        <List empty={<Empty />} actions={<RoleListActions />}>
            <Datagrid>
            <TextField source="id" />
            <TextField source="name" />
            <TextField source="authority" />
                    <RowButtonGroup label="⋮">
                        <DropDownButton>
                            <ShowButton />
                            <EditButton />
                            <ExportRecordButton />
                            <DeleteWithDialogButton />
                        </DropDownButton>
                    </RowButtonGroup>
            </Datagrid>
        </List>
    );
};
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
