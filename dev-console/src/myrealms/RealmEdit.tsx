import { Alert, Box } from '@mui/material';
import {
    BooleanInput,
    Edit,
    List,
    SaveButton,
    TabbedForm,
    TextInput,
    Toolbar,
    TopToolbar,
    useRecordContext,
    useTranslate,
} from 'react-admin';
import { InspectButton } from '@dslab/ra-inspect-button';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { Page } from '../components/Page';
import { SectionTitle } from '../components/SectionTitle';
import { RefreshingExportButton } from '../components/RefreshingExportButton';
import { ResourceTitle } from '../components/ResourceTitle';
import SettingsIcon from '@mui/icons-material/Settings';
import {
    realmLocalizationSchema,
    realmOAuthSchema,
    realmTosSchema,
} from './schemas';
import { JsonSchemaInput } from '@dslab/ra-jsonschema-input';
import WarningIcon from '@mui/icons-material/WarningOutlined';
import {
    DeveloperListActions,
    DeveloperListView,
} from '../developers/DeveloperList';
import { AceEditorInput } from '@dslab/ra-ace-editor';

export const RealmEdit = () => {
    return (
        <Page>
            <Edit
                actions={<ActionsToolbar />}
                mutationMode="optimistic"
                component={Box}
                redirect={'edit'}
            >
                <ResourceTitle
                    text={'configuration'}
                    icon={
                        <SettingsIcon
                            fontSize="large"
                            sx={{ fontSize: '96px' }}
                            color="secondary"
                        />
                    }
                />
                <RealmEditForm />
            </Edit>
        </Page>
    );
};

const RealmEditForm = () => {
    const translate = useTranslate();
    const record = useRecordContext();
    if (!record) return null;

    if (!record.editable) {
        return (
            <Alert severity="warning" icon={<WarningIcon />}>
                {translate('error.not_editable')}
            </Alert>
        );
    }

    return (
        <TabbedForm toolbar={<TabToolbar />} syncWithLocation={false}>
            <TabbedForm.Tab label="tab.settings">
                <SectionTitle
                    text={translate('page.realm.settings.header.title')}
                    secondaryText={translate(
                        'page.realm.settings.header.subtitle'
                    )}
                />
                <TextInput
                    source="slug"
                    label="field.slug.name"
                    helperText="field.slug.helperText"
                    fullWidth
                    readOnly
                />
                <TextInput
                    source="name"
                    label="field.name.name"
                    helperText="field.name.helperText"
                    fullWidth
                />
                <TextInput
                    source="description"
                    label="field.description.name"
                    helperText="field.description.helperText"
                    multiline
                    fullWidth
                />
                <BooleanInput
                    source="public"
                    label="field.public.name"
                    helperText="field.public.helperText"
                />
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.localization">
                <SectionTitle
                    text={translate('page.realm.localization.header.title')}
                    secondaryText={translate(
                        'page.realm.localization.header.subtitle'
                    )}
                />
                <JsonSchemaInput
                    source="localizationConfiguration"
                    schema={realmLocalizationSchema}
                />
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.templates">
                <SectionTitle
                    text={translate('page.realm.templates.header.title')}
                    secondaryText={translate(
                        'page.realm.templates.header.subtitle'
                    )}
                />
                <AceEditorInput
                    source="templatesConfiguration.customStyle"
                    label="field.customStyle.name"
                    helperText="field.customStyle.helperText"
                    mode="css"
                    minLines={12}
                />
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.oauth2">
                <SectionTitle
                    text={translate('page.realm.oauth2.header.title')}
                    secondaryText={translate(
                        'page.realm.oauth2.header.subtitle'
                    )}
                />
                <JsonSchemaInput
                    source="oauthConfiguration"
                    schema={realmOAuthSchema}
                />
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.tos">
                <SectionTitle
                    text={translate('page.realm.tos.header.title')}
                    secondaryText={translate('page.realm.tos.header.subtitle')}
                />
                <JsonSchemaInput
                    source="tosConfiguration"
                    schema={realmTosSchema}
                />
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.developers">
                <SectionTitle
                    text={translate('page.realm.developers.header.title')}
                    secondaryText={translate(
                        'page.realm.roles.developers.subtitle'
                    )}
                />
                <List
                    resource="developers"
                    exporter={false}
                    actions={<DeveloperListActions />}
                    sort={{ field: 'name', order: 'ASC' }}
                    component={Box}
                    empty={false}
                    disableSyncWithLocation
                    storeKey={false}
                >
                    <DeveloperListView />
                </List>
            </TabbedForm.Tab>
        </TabbedForm>
    );
};

const ActionsToolbar = () => {
    const record = useRecordContext();
    if (!record || !record.editable) return null;

    return (
        <TopToolbar>
            <InspectButton />
            <DeleteWithDialogButton />
            <RefreshingExportButton />
        </TopToolbar>
    );
};

const TabToolbar = () => (
    <Toolbar>
        <SaveButton />
    </Toolbar>
);
