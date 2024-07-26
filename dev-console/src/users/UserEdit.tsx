import { Box, Divider } from '@mui/material';
import {
    Edit,
    ReferenceArrayInput,
    SaveButton,
    ShowButton,
    TabbedForm,
    TextField,
    TextInput,
    Toolbar,
    TopToolbar,
    useEditContext,
    useNotify,
    useRecordContext,
    useRefresh,
    useTranslate,
} from 'react-admin';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { JsonSchemaInput } from '@dslab/ra-jsonschema-input';
import { InspectButton } from '@dslab/ra-inspect-button';
import { PageTitle } from '../components/pageTitle';
import { SectionTitle } from '../components/sectionTitle';
import { schemaOAuthClient, uiSchemaOAuthClient } from '../common/schemas';
import { Page } from '../components/page';
import { ActiveButton } from './activeButton';
import { TabToolbar } from '../components/TabToolbar';

export const UserEdit = () => {
    return (
        <Page>
            <Edit
                actions={<EditToolBarActions />}
                mutationMode="pessimistic"
                component={Box}
            >
                <UserTabComponent />
            </Edit>
        </Page>
    );
};


const UserTabComponent = () => {
    const translate = useTranslate();
    const { isLoading, record } = useEditContext<any>();
    if (isLoading || !record) return null;
    if (!record) return null;
    return (
        <>
            <PageTitle text={record.username} secondaryText={record?.id} />
            <TabbedForm  toolbar={<TabToolbar />}>
                <TabbedForm.Tab label={translate('page.user.overview.title')}>
                <TextField source="username" />
                    <TextField source="email" />
                    <TextField source="subjectId" />
                    <TextField source="roles" />
                    <TextField source="permissions" />
                </TabbedForm.Tab>
                <TabbedForm.Tab label={translate('page.user.account.title')}>
                    <TextField source="id" />
                    <TextField source="email" />
                </TabbedForm.Tab>
                <TabbedForm.Tab label={translate('page.user.audit.title')}>
                    <TextField source="id" />
                </TabbedForm.Tab>
                <TabbedForm.Tab label={translate('page.user.apps.title')}>
                    <TextField source="id" />
                </TabbedForm.Tab>
                <TabbedForm.Tab label={translate('page.user.groups.title')}>
                    <TextField source="id" />
                </TabbedForm.Tab>
                <TabbedForm.Tab label={translate('page.user.roles.title')}>
                    <TextField source="id" />
                </TabbedForm.Tab>
                <TabbedForm.Tab label={translate('page.user.attributes.title')}>
                    <TextField source="id" />
                </TabbedForm.Tab>
                <TabbedForm.Tab label={translate('page.user.spaceRoles.title')}>
                    <TextField source="id" />
                </TabbedForm.Tab>
                <TabbedForm.Tab label={translate('page.user.tos.title')}>
                    <TextField source="id" />
                </TabbedForm.Tab>
            </TabbedForm>
        </>
    );
};


const EditToolBarActions = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <TopToolbar>
            <ActiveButton />
            <ShowButton />
            <InspectButton />
            <DeleteWithDialogButton />
            <ExportRecordButton language="yaml" color="info" />
            
        </TopToolbar>
    );
};


