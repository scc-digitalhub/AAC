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
import { TabToolbar } from '../components/TabToolbar';

export const AppEdit = () => {
    return (
        <Page>
            <Edit
                actions={<EditToolBarActions />}
                mutationMode="pessimistic"
                component={Box}
            >
                <AppTabComponent />
            </Edit>
        </Page>
    );
};


const AppTabComponent = () => {
    const translate = useTranslate();
    const notify = useNotify();
    const refresh = useRefresh();
    const { isLoading, record } = useEditContext<any>();
    if (isLoading || !record) return null;
    if (!record) return null;
    return (
        <>
            <PageTitle text={record.name} secondaryText={record?.id} copy={true}/>
            <TabbedForm toolbar={<TabToolbar />}>
                <TabbedForm.Tab label={translate('page.app.overview.title')}>
                    <TextField source="name" />
                    <TextField source="type" />
                    <TextField source="clientId" />
                    <TextField source="scopes" />
                </TabbedForm.Tab>
                <TabbedForm.Tab label={translate('page.app.settings.title')}>
                    <EditSetting />
                </TabbedForm.Tab>
                <TabbedForm.Tab
                    label={translate('page.app.configuration.title')}
                >
                    <SectionTitle
                        text={translate('page.app.configuration.header.title')}
                        secondaryText={translate(
                            'page.app.configuration.header.subtitle'
                        )}
                    />
                    <TextInput source="clientId" readOnly/>
                    <EditOAuthJsonSchemaForm />
                </TabbedForm.Tab>
                <TabbedForm.Tab label="Providers">
                    <ReferenceArrayInput source="providers" reference="idps" />
                </TabbedForm.Tab>
                <TabbedForm.Tab label="Scopes">
                    <ReferenceArrayInput source="scopes" reference="scopes" />
                    {/* todo getlist di risorse scope, click con dialog che aggiunge in scope */}
                </TabbedForm.Tab>
            </TabbedForm>
        </>
    );
};

const EditSetting = () => {
    const notify = useNotify();
    const refresh = useRefresh();
    const { isLoading, record } = useEditContext<any>();
    if (isLoading || !record) return null;
    return (
        <Box>
            <Box display="flex">
                <Box flex="1" mt={-1}>
                    <Box display="flex" width={430}>
                        <TextInput source="name" fullWidth />
                    </Box>
                    <Box display="flex" width={430}>
                        <TextInput source="description" fullWidth />
                    </Box>
                    <Divider />
                </Box>
            </Box>
        </Box>
    );
};

const EditToolBarActions = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <TopToolbar>
            <ShowButton />
            <InspectButton />
            <DeleteWithDialogButton />
            <ExportRecordButton language="yaml" color="info" />
        </TopToolbar>
    );
};

const EditOAuthJsonSchemaForm = () => {
    const { isLoading, record } = useEditContext<any>();
    if (isLoading || !record) return null;

    return (
        <JsonSchemaInput
            source="configuration"
            schema={schemaOAuthClient}
            uiSchema={uiSchemaOAuthClient}
        />
    );
};
