import { Box } from '@mui/material';
import {
    BooleanInput,
    Edit,
    Labeled,
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
    realmTosSchema,
} from './schemas';
import { JsonSchemaInput } from '@dslab/ra-jsonschema-input';

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
            <TabbedForm.Tab label="tab.members">
                <SectionTitle
                    text={translate('page.realm.members.header.title')}
                    secondaryText={translate(
                        'page.realm.roles.members.subtitle'
                    )}
                />
                <ReferenceArrayInput source="members" reference="subjects">
                    <DatagridArrayInput
                        dialogFilters={[<TextInput label="query" source="q" />]}
                        dialogFilterDefaultValues={{ q: '' }}
                    >
                        <TextField source="name" />
                        <TextField source="type" />
                        <IdField source="id" />
                    </DatagridArrayInput>
                </ReferenceArrayInput>
            </TabbedForm.Tab>
        </TabbedForm>
    );
};

const RealmToolBarActions = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <TopToolbar>
            <InspectButton />
            <DeleteWithDialogButton />
            <RefreshingExportButton />
        </TopToolbar>
    );
};
