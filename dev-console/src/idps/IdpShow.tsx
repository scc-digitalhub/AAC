import { Box } from '@mui/material';
import {
    Datagrid,
    DateField,
    EditButton,
    ReferenceManyField,
    RichTextField,
    Show,
    SimpleForm,
    TabbedShowLayout,
    TextField,
    TopToolbar,
    useEditContext,
    useNotify,
    useRecordContext,
    useRefresh,
    useTranslate,
} from 'react-admin';
import { PageTitle } from '../components/pageTitle';
import { SectionTitle } from '../components/sectionTitle';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { InspectButton } from '@dslab/ra-inspect-button';
import { JsonSchemaField } from '@dslab/ra-jsonschema-input';
import { schemaOAuthClient, uiSchemaOAuthClient } from '../common/schemas';
import { Page } from '../components/page';

export const IdpShow = () => {
    return (
        <Page>
            <Show actions={<ShowToolBarActions />} component={Box}>
                <IdpTabComponent />
            </Show>
        </Page>
    );
};

const IdpTabComponent = () => {
    const record = useRecordContext();
    const translate = useTranslate();
    if (!record) return null;

    return (
        <>
            <PageTitle text={record.name} secondaryText={record?.id} />
            <TabbedShowLayout syncWithLocation={false}>
                <TabbedShowLayout.Tab
                    label={translate('page.idp.overview.title')}
                >
                    <TextField source="name" />
                    <TextField source="type" />
                    <TextField source="authority" />
                    <TextField source="provider" />
                    <TextField source="enabled" />
                    <TextField source="registered" />
                    
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.idp.settings.title')}
                >
                    <OAuthJsonSchemaForm />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.idp.configuration.title')}
                >
                   
                    <OAuthJsonSchemaForm />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.idp.hooks.title')}
                >
                    
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.idp.app.title')}
                >
                    <ReferenceManyField reference="apps" target="providers" label="app">
              <Datagrid >
                <TextField source="id" />
                <TextField source="name" />
              </Datagrid>
            </ReferenceManyField>
                </TabbedShowLayout.Tab>
            </TabbedShowLayout>
        </>
    );
};
const OAuthJsonSchemaForm = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <JsonSchemaField
            source="configuration"
            schema={schemaOAuthClient}
            uiSchema={uiSchemaOAuthClient}
        />
    );
};
const ShowToolBarActions = () => {
    const record = useRecordContext();
    if (!record) return null;
    return (
        <TopToolbar>
            <EditButton />
            <InspectButton />
            <DeleteWithDialogButton />
            <ExportRecordButton language="yaml" color="info" />
        </TopToolbar>
    );
};
