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
import { ServiceCreateForm } from './ServiceCreate';
import { Box, Typography } from '@mui/material';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { DropDownButton } from '../components/DropdownButton';
import { IdField } from '../components/IdField';
import { RowButtonGroup } from '../components/RowButtonGroup';
import { EnableIdpButton } from '../idps/IdpList';

export const ServiceList = () => {
    return (
        <List empty={<Empty />} actions={<ServiceListActions />}>
            <Datagrid>
            <TextField source="id" />
            <TextField source="name" />
            <TextField source="namespace" />
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
                No Service available, create one
            </Typography>
            <CreateInDialogButton
                fullWidth
                maxWidth={'md'}
                variant="contained"
                transform={createTransform}
            >
                <ServiceCreateForm />
            </CreateInDialogButton>
        </Box>
    );
};
const createTransform = (data: any) => {
    return {
        ...data,
    };
};
const ServiceListActions = () => {
    return (
        <TopToolbar>
            <CreateInDialogButton
                fullWidth
                maxWidth={'md'}
                variant="contained"
                transform={createTransform}
            >
                <ServiceCreateForm />
            </CreateInDialogButton>
        </TopToolbar>
    );
};
