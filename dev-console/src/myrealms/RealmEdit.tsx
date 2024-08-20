import { Alert, Box } from '@mui/material';
import {
    BooleanInput,
    Edit,
    Labeled,
    List,
    NumberField,
    ReferenceArrayInput,
    TabbedForm,
    TextField,
    TextInput,
    TopToolbar,
    useGetList,
    useRecordContext,
    useTranslate,
} from 'react-admin';
import { InspectButton } from '@dslab/ra-inspect-button';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { IdField } from '../components/IdField';
import { Page } from '../components/Page';
import { TabToolbar } from '../components/TabToolbar';
import { DatagridArrayInput } from '@dslab/ra-datagrid-input';
import { SectionTitle } from '../components/sectionTitle';
import { RefreshingExportButton } from '../components/RefreshingExportButton';
import { ResourceTitle } from '../components/ResourceTitle';
import SettingsIcon from '@mui/icons-material/Settings';
import {
    realmLocalizationSchema,
    realmOAuthSchema,
    realmTemplatesSchema,
    realmTosSchema,
} from './schemas';
import { JsonSchemaInput } from '@dslab/ra-jsonschema-input';
import WarningIcon from '@mui/icons-material/WarningOutlined';
import { AceEditorInput } from '../components/AceEditorInput';
import {
    DeveloperFilters,
    DeveloperListActions,
    DeveloperListView,
} from '../developers/DeveloperList';

export const RealmEdit = () => {
    return (
        <Page>
            <Edit
                actions={<RealmToolBarActions />}
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
                <TextInput source="slug" fullWidth readOnly />
                <TextInput source="name" fullWidth />
                <TextInput source="description" multiline fullWidth />
                <BooleanInput source="public" />
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
                    label="field.customStyle"
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

const RealmToolBarActions = () => {
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
