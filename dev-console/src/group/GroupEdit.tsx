import {
    Box,
    Card,
    CardContent,
    Divider,
    IconButton,
    Typography,
} from '@mui/material';
import {
    Button,
    Edit,
    EditBase,
    Form,
    ReferenceArrayInput,
    SaveButton,
    ShowButton,
    TabbedForm,
    TabbedShowLayout,
    TextField,
    TextInput,
    Toolbar,
    TopToolbar,
    useEditContext,
    useNotify,
    useRecordContext,
    useRedirect,
    useRefresh,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import React from 'react';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { RJSFSchema, UiSchema } from '@rjsf/utils';
import { InspectButton } from '@dslab/ra-inspect-button';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { IdField } from '../components/IdField';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { Page } from '../components/page';
import { PageTitle } from '../components/pageTitle';
import { TabToolbar } from '../components/TabToolbar';
import { DatagridArrayInput } from '@dslab/ra-datagrid-input';

export const GroupEdit = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    return (
        <Page>
            <Edit
                actions={<GroupToolBarActions />}
                mutationMode="pessimistic"
                queryOptions={options}
                component={Box}
            >
                <GroupTabComponent />
            </Edit>
        </Page>
    );
};

const GroupTabComponent = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <>
            <PageTitle
                text={record.name}
                secondaryText={record?.id}
                copy={true}
            />
            <TabbedForm toolbar={<TabToolbar />}>
                <TabbedForm.Tab label="Overview">
                    <TextField source="id" />
                    <TextField source="name" />
                    <TextField source="group" />
                    <TextField source="members" />
                </TabbedForm.Tab>
                <TabbedForm.Tab label="Settings">
                    <TextInput source="name" fullWidth />
                    <TextInput source="group" fullWidth />
                    <TextInput source="description" multiline fullWidth />
                </TabbedForm.Tab>
                <TabbedForm.Tab label="Roles">
                    <TextField source="id" />
                </TabbedForm.Tab>
                <TabbedForm.Tab label="Members">
                    <ReferenceArrayInput source="members" reference="subjects">
                        <DatagridArrayInput
                            dialogFilters={[
                                <TextInput label="query" source="q" />,
                            ]}
                            dialogFilterDefaultValues={{ q: '' }}
                        >
                            <TextField source="name" />
                            <TextField source="type" />
                            <IdField source="id" />
                        </DatagridArrayInput>
                    </ReferenceArrayInput>
                </TabbedForm.Tab>
            </TabbedForm>
        </>
    );
};

const GroupToolBarActions = () => {
    const record = useRecordContext();
    if (!record) return null;
    return (
        <TopToolbar>
            <ShowButton />
            <InspectButton />
            <DeleteWithDialogButton />
            <ExportRecordButton />
        </TopToolbar>
    );
};
