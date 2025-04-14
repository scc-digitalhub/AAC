import { Box, Grid } from '@mui/material';
import {
    AutocompleteArrayInput,
    CheckboxGroupInput,
    DeleteWithConfirmButton,
    Edit,
    Labeled,
    List,
    ReferenceArrayInput,
    SaveButton,
    ShowButton,
    TabbedForm,
    TextField,
    TextInput,
    Toolbar,
    TopToolbar,
    useDataProvider,
    useEditContext,
    useGetList,
    useRecordContext,
    useRefresh,
} from 'react-admin';
import { JsonSchemaInput } from '@dslab/ra-jsonschema-input';
import { InspectButton } from '@dslab/ra-inspect-button';
import { SectionTitle } from '../components/SectionTitle';
import {
    claimMappingDefaultValue,
    schemaOAuthClient,
    schemaWebHooks,
    uiSchemaOAuthClient,
} from './schemas';
import { Page } from '../components/Page';
import { AuthoritiesDialogButton } from '../components/AuthoritiesDialog';
import { TestDialogButton } from './TestDialog';
import { RefreshingExportButton } from '../components/RefreshingExportButton';
import { IdpNameField } from '../idps/IdpList';
import { AppResources } from './AppResources';
import { AppTitle } from './AppShow';
import { ClaimMappingEditor } from '../components/ClaimMappingEditor';
import { useRootSelector } from '@dslab/ra-root-selector';
import { IdField } from '../components/IdField';
import { ConfirmDialogButton } from '../components/ConfirmDialog';
import LockResetIcon from '@mui/icons-material/LockReset';
import { PasswordField } from '../components/PasswordField';

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
                actions={<ActionsToolbar />}
                mutationMode="optimistic"
                component={Box}
                redirect={'edit'}
                transform={transform}
                queryOptions={{ meta: { flatten: ['roles', 'groups'] } }}
                mutationOptions={{ meta: { flatten: ['roles', 'groups'] } }}
            >
                <AppTitle />
                <AppEditForm />
            </Edit>
        </Page>
    );
};

const AppEditForm = () => {
    const dataProvider = useDataProvider();
    const { root: realmId } = useRootSelector();
    const refresh = useRefresh();
    const { isLoading, record } = useEditContext<any>();
    if (isLoading || !record) return null;
    if (!record) return null;

    const handleTest = (record, code) => {
        return dataProvider.invoke({
            path: 'apps/' + realmId + '/' + record.id + '/claims',
            body: JSON.stringify({
                code,
                name: 'claimMapping',
                scopes: [],
            }),
            options: {
                method: 'POST',
            },
        });
    };

    const resetCredentials = type => {
        return dataProvider
            .invoke({
                path:
                    'apps/' +
                    realmId +
                    '/' +
                    record.id +
                    '/credentials/' +
                    record.id +
                    '_' +
                    type,
                body: JSON.stringify({}),
                options: {
                    method: 'PUT',
                },
            })
            .then(r => {
                refresh();
            });
    };

    const resetClientSecret = () => resetCredentials('credentials_secret');
    const resetClientJwks = () => resetCredentials('credentials_jwks');

    const hasClientSecret =
        record.configuration.clientSecret ||
        record.configuration?.authenticationMethods?.includes(
            'client_secret_post'
        ) ||
        record.configuration?.authenticationMethods?.includes(
            'client_secret_basic'
        );
    const hasClientJwks =
        record.configuration.jwks ||
        record.configuration?.authenticationMethods?.includes(
            'private_key_jwt'
        );
    return (
        <TabbedForm toolbar={<TabToolbar />} syncWithLocation={false}>
            <TabbedForm.Tab label="tab.overview">
                <Labeled>
                    <TextField source="name" label="field.name.name" />
                </Labeled>
                <Labeled>
                    <TextField source="type" label="field.type.name" />
                </Labeled>
                <Labeled>
                    <TextField source="clientId" label="field.clientId.name" />
                </Labeled>
                <Labeled>
                    <TextField source="scopes" label="field.scopes.name" />
                </Labeled>
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.settings">
                <SectionTitle
                    text="page.apps.settings.header.title"
                    secondaryText="page.apps.settings.header.subtitle"
                />
                <TextInput
                    source="name"
                    fullWidth
                    label="field.name.name"
                    helperText="field.name.helperText"
                />
                <TextInput
                    source="description"
                    fullWidth
                    label="field.description.name"
                    helperText="field.description.helperText"
                />
                <TextInput
                    source="notes"
                    fullWidth
                    label="field.notes.name"
                    helperText="field.notes.helperText"
                />
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.credentials">
                <SectionTitle
                    text="page.apps.credentials.header.title"
                    secondaryText="page.apps.credentials.header.subtitle"
                />
                <Grid container gap={1}>
                    <Grid item xs={12}>
                        <Labeled>
                            <IdField
                                source="clientId"
                                label="field.clientId.name"
                            />
                        </Labeled>
                    </Grid>
                    {hasClientSecret && (
                        <Grid item xs={12}>
                            <Labeled>
                                <PasswordField
                                    source="configuration.clientSecret"
                                    label="field.clientSecret.name"
                                >
                                    <ConfirmDialogButton
                                        onConfirm={resetClientSecret}
                                        icon={<LockResetIcon />}
                                        title={'page.credentials.reset.title'}
                                        label={''}
                                        helperText="page.credentials.reset.helperText"
                                        color="error"
                                    />
                                </PasswordField>
                            </Labeled>
                        </Grid>
                    )}
                    {hasClientJwks && (
                        <Grid item xs={12}>
                            <Labeled>
                                <IdField
                                    source="configuration.jwks"
                                    label="field.jwks.name"
                                    format={value =>
                                        value && value.length > 120
                                            ? value.substring(0, 120) + '...'
                                            : value
                                    }
                                >
                                    <ConfirmDialogButton
                                        onConfirm={resetClientJwks}
                                        icon={<LockResetIcon />}
                                        title={'page.credentials.reset.title'}
                                        label={''}
                                        helperText="page.credentials.reset.helperText"
                                        color="error"
                                    />
                                </IdField>
                            </Labeled>
                        </Grid>
                    )}
                </Grid>
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
            <TabbedForm.Tab label="tab.providers">
                <SectionTitle
                    text="page.apps.providers.header.title"
                    secondaryText="page.apps.providers.header.subtitle"
                />
                <ReferenceArrayInput
                    source="providers"
                    label="field.providers.name"
                    helperText="field.providers.helperText"
                    reference="idps"
                    sort={{ field: 'name', order: 'ASC' }}
                >
                    <CheckboxGroupInput
                        label="field.providers.name"
                        helperText="field.providers.helperText"
                        row={false}
                        labelPlacement="end"
                        optionText={<IdpNameField source="name" />}
                        sx={{ textAlign: 'left' }}
                    />
                </ReferenceArrayInput>
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.api_access">
                <SectionTitle
                    text="page.apps.scopes.header.title"
                    secondaryText="page.apps.scopes.header.subtitle"
                />
                <ReferenceArrayInput
                    source="scopes"
                    reference="scopes"
                    label="field.scopes.name"
                    helperText="field.scopes.helperText"
                >
                    <AutocompleteArrayInput
                        label="field.scopes.name"
                        helperText="field.scopes.helperText"
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

                <ClaimMappingEditor
                    source="hookFunctions.claimMapping"
                    onTest={handleTest}
                    defaultValue={claimMappingDefaultValue}
                />
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
                    label="field.roles.name"
                    helperText="field.roles.helperText"
                    sort={{ field: 'role', order: 'ASC' }}
                >
                    <AutocompleteArrayInput
                        label="field.roles.name"
                        helperText="field.roles.helperText"
                        optionText={'role'}
                        optionValue={'id'}
                    />
                </ReferenceArrayInput>
            </TabbedForm.Tab>

            <TabbedForm.Tab label="tab.groups">
                <SectionTitle
                    text="page.apps.groups.header.title"
                    secondaryText="page.apps.groups.header.subtitle"
                />
                <ReferenceArrayInput
                    source="groups"
                    reference="groups"
                    label="field.groups.name"
                    helperText="field.groups.helperText"
                    sort={{ field: 'group', order: 'ASC' }}
                >
                    <AutocompleteArrayInput
                        label="field.groups.name"
                        helperText="field.groups.helperText"
                        optionText={'group'}
                        optionValue={'id'}
                    />
                </ReferenceArrayInput>
            </TabbedForm.Tab>
        </TabbedForm>
    );
};

const ActionsToolbar = () => {
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

const TabToolbar = () => (
    <Toolbar>
        <SaveButton alwaysEnable />
    </Toolbar>
);
