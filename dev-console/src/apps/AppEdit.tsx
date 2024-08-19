import { Box } from '@mui/material';
import {
    AutocompleteArrayInput,
    CheckboxGroupInput,
    DeleteWithConfirmButton,
    Edit,
    Labeled,
    List,
    ReferenceArrayInput,
    ShowButton,
    TabbedForm,
    TextField,
    TextInput,
    TopToolbar,
    useEditContext,
    useGetList,
    useRecordContext,
    useRefresh,
} from 'react-admin';
import { JsonSchemaInput } from '@dslab/ra-jsonschema-input';
import { InspectButton } from '@dslab/ra-inspect-button';
import { SectionTitle } from '../components/sectionTitle';
import {
    schemaOAuthClient,
    schemaWebHooks,
    uiSchemaOAuthClient,
} from './schemas';
import { Page } from '../components/Page';
import { TabToolbar } from '../components/TabToolbar';
import { AuthoritiesDialogButton } from '../components/AuthoritiesDialog';
import { TestDialogButton } from './TestDialog';
import { RefreshingExportButton } from '../components/RefreshingExportButton';
import { ResourceTitle } from '../components/ResourceTitle';
import { IdpNameField } from '../idps/IdpList';
import { AppResources } from './AppResources';
import { ClaimMappingEditor } from './ClaimMappingEditor';

export const AppEdit = () => {
    //fetch related to resolve relations
    const { data: roles } = useGetList('roles', {
        pagination: { page: 1, perPage: 100 },
        sort: { field: 'name', order: 'ASC' },
    });
    const { data: groups } = useGetList('groups', {
        pagination: { page: 1, perPage: 100 },
        sort: { field: 'name', order: 'ASC' },
    });

    //inflate back flattened fields
    const transform = data => ({
        ...data,
        roles: roles?.filter(r => data.roles.includes(r.id)),
        groups: groups?.filter(r => data.groups.includes(r.id)),
    });

    return (
        <Page>
            <Edit
                actions={<EditToolBarActions />}
                mutationMode="optimistic"
                component={Box}
                redirect={'edit'}
                transform={transform}
                queryOptions={{ meta: { flatten: ['roles', 'groups'] } }}
                mutationOptions={{ meta: { flatten: ['roles', 'groups'] } }}
            >
                <ResourceTitle />
                <AppEditForm />
            </Edit>
        </Page>
    );
};

const AppEditForm = () => {
    const { isLoading, record } = useEditContext<any>();
    if (isLoading || !record) return null;
    if (!record) return null;
    return (
        <TabbedForm toolbar={<TabToolbar />} syncWithLocation={false}>
            <TabbedForm.Tab label="tab.overview">
                <Labeled>
                    <TextField source="name" />
                </Labeled>
                <Labeled>
                    <TextField source="type" />
                </Labeled>
                <Labeled>
                    <TextField source="clientId" />
                </Labeled>
                <Labeled>
                    <TextField source="scopes" />
                </Labeled>
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.settings">
                <SectionTitle
                    text="page.apps.settings.header.title"
                    secondaryText="page.apps.settings.header.subtitle"
                />
                <TextInput source="name" fullWidth />
                <TextInput source="description" fullWidth />
            </TabbedForm.Tab>

            <TabbedForm.Tab label="tab.providers">
                <SectionTitle
                    text="page.apps.providers.header.title"
                    secondaryText="page.apps.providers.header.subtitle"
                />
                {/* <ReferenceArrayInput
                    source="providers"
                    reference="idps"
                    sort={{ field: 'name', order: 'ASC' }}
                /> */}
                <ReferenceArrayInput
                    source="providers"
                    reference="idps"
                    sort={{ field: 'name', order: 'ASC' }}
                >
                    <CheckboxGroupInput
                        row={false}
                        labelPlacement="end"
                        optionText={<IdpNameField source="name" />}
                        sx={{ textAlign: 'left' }}
                    />
                </ReferenceArrayInput>
            </TabbedForm.Tab>

            <TabbedForm.Tab label="tab.configuration">
                <SectionTitle
                    text="page.apps.configuration.header.title"
                    secondaryText="page.apps.configuration.header.subtitle"
                />
                <JsonSchemaInput
                    source="configuration"
                    schema={schemaOAuthClient}
                    uiSchema={uiSchemaOAuthClient}
                />
            </TabbedForm.Tab>

            <TabbedForm.Tab label="tab.api_access">
                <SectionTitle
                    text="page.apps.scopes.header.title"
                    secondaryText="page.apps.scopes.header.subtitle"
                />
                <ReferenceArrayInput source="scopes" reference="scopes">
                    <AutocompleteArrayInput
                        label="scopes"
                        optionText={'id'}
                        optionValue={'id'}
                    />
                </ReferenceArrayInput>
                <List
                    resource="resources"
                    component={Box}
                    actions={false}
                    pagination={false}
                >
                    <AppResources />
                </List>
            </TabbedForm.Tab>

            <TabbedForm.Tab label="tab.hooks">
                <SectionTitle
                    text="page.apps.hooks.claimMapping.title"
                    secondaryText="page.apps.hooks.claimMapping.subtitle"
                />

                <ClaimMappingEditor />

                <SectionTitle
                    text="page.apps.hooks.webhooks.title"
                    secondaryText="page.apps.hooks.webhooks.subtitle"
                />

                <JsonSchemaInput source="hookWebUrls" schema={schemaWebHooks} />
            </TabbedForm.Tab>

            <TabbedForm.Tab label="tab.roles">
                <SectionTitle
                    text="page.apps.roles.header.title"
                    secondaryText="page.apps.roles.header.subtitle"
                />
                <ReferenceArrayInput
                    source="roles"
                    reference="roles"
                    sort={{ field: 'role', order: 'ASC' }}
                />
            </TabbedForm.Tab>

            <TabbedForm.Tab label="tab.groups">
                <SectionTitle
                    text="page.apps.groups.header.title"
                    secondaryText="page.apps.groups.header.subtitle"
                />
                <ReferenceArrayInput
                    source="groups"
                    reference="groups"
                    sort={{ field: 'group', order: 'ASC' }}
                />
            </TabbedForm.Tab>
        </TabbedForm>
    );
};

const EditToolBarActions = () => {
    const refresh = useRefresh();
    const record = useRecordContext();

    if (!record) return null;

    return (
        <TopToolbar>
            <TestDialogButton />
            <ShowButton />
            <InspectButton />
            <AuthoritiesDialogButton onSuccess={() => refresh()} />
            <DeleteWithConfirmButton />
            <RefreshingExportButton />
        </TopToolbar>
    );
};
