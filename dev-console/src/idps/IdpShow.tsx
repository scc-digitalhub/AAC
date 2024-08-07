import { Box } from '@mui/material';
import {
    BooleanField,
    Datagrid,
    DateField,
    EditButton,
    RecordContextProvider,
    ReferenceManyField,
    RichTextField,
    Show,
    SimpleForm,
    TabbedShowLayout,
    TextField,
    TopToolbar,
    TranslatableFields,
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
import {
    getUiSchema,
    schemaOAuthClient,
    uiSchemaOAuthClient,
} from '../common/schemas';
import { Page } from '../components/page';
import { AceEditorField } from '../components/AceEditorField';

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
            <PageTitle text={record.name} secondaryText={record?.id} copy={true}/>
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
                    <SectionTitle text={translate('page.idp.settings.basic')} />
                    <TextField source="name" />
                    <SectionTitle
                        text={translate('page.idp.settings.display')}
                    />
                    <TranslatableFields locales={['it', 'en', 'de']}>
                        <TextField source="titleMap" />
                        <TextField source="descriptionMap" multiline />
                    </TranslatableFields>
                    <SectionTitle
                        text={translate('page.idp.settings.advanced')}
                    />

                    <TextField source="settings.events" label="Events" />
                    <TextField
                        source="settings.persistence"
                        label="Persistence"
                    />
                    <BooleanField source="settings.linkable" />
                    <TextField source="settings.type" />
                    <TextField source="settings.persistence" />
                    <BooleanField source="settings.linkable" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.idp.configuration.title')}
                >
                    <JsonSchemaField
                        source="configuration"
                        schema={record.schema}
                        uiSchema={getUiSchema(record?.schema?.id)}
                    />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label={translate('page.idp.hooks.title')}>
                <RecordContextProvider value={record?.settings.hookFunctions}>
                <SectionTitle
                        text={translate('page.idp.hooks.attribute')}
                        secondaryText={translate('page.idp.hooks.attributeDesc')}
                    />
                    <Box>
                        <AceEditorField
                            source="attributeMapping"
                            mode="yaml"
                            theme="github"
                        ></AceEditorField>
                    </Box>
                    <SectionTitle
                        text={translate('page.idp.hooks.authFunction')}
                        secondaryText={translate('page.idp.hooks.authFunctionDesc')}
                    />
                    <Box>
                        <AceEditorField
                            source="authorize"
                            mode="yaml"
                            theme="github"
                        ></AceEditorField>
                        
                    </Box>
                    </RecordContextProvider>
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label={translate('page.idp.app.title')}>
                    <ReferenceManyField
                        reference="apps"
                        target="providers"
                        label="app"
                    >
                        <Datagrid bulkActionButtons={false}>
                            <TextField source="id" />
                            <TextField source="name" />
                        </Datagrid>
                    </ReferenceManyField>
                </TabbedShowLayout.Tab>
            </TabbedShowLayout>
        </>
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
