import { Box, Grid } from '@mui/material';
import {
    Button,
    DeleteWithConfirmButton,
    Edit,
    Labeled,
    ReferenceArrayInput,
    ShowButton,
    TabbedForm,
    TextField,
    TextInput,
    TopToolbar,
    useDataProvider,
    useEditContext,
    useGetList,
    useGetOne,
    useNotify,
    useRecordContext,
    useRefresh,
    useTranslate,
} from 'react-admin';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { JsonSchemaInput } from '@dslab/ra-jsonschema-input';
import { InspectButton } from '@dslab/ra-inspect-button';
import { SectionTitle } from '../components/sectionTitle';
import {
    claimMappingDefaultValue,
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
import utils from '../utils';
import { AceEditorInput } from '../components/AceEditorInput';
import { ControlledEditorInput } from '../components/ControllerEditorInput';
import { useWatch } from 'react-hook-form';
import { useRootSelector } from '@dslab/ra-root-selector';
import { useEffect, useState } from 'react';
import TestIcon from '@mui/icons-material/DirectionsRun';
import { AceEditorField } from '../components/AceEditorField';

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
    const translate = useTranslate();
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
                <ReferenceArrayInput
                    source="providers"
                    reference="idps"
                    sort={{ field: 'name', order: 'ASC' }}
                />
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
                <ReferenceArrayInput source="scopes" reference="scopes" />
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

const ClaimMappingEditor = () => {
    const dataProvider = useDataProvider();
    const { root: realmId } = useRootSelector();
    const record = useRecordContext();
    const notify = useNotify();
    const translate = useTranslate();
    const code = useWatch({
        name: 'hookFunctions.claimMapping',
        defaultValue: null,
    });
    const defaultValue = {
        context: null,
        result: null,
        errors: [],
        messages: null,
    };

    const [result, setResult] = useState<any>(defaultValue);

    useEffect(() => {
        if (code == null) {
            setResult(defaultValue);
        }
    }, [code]);

    const handleTest = e => {
        if (code && record && realmId) {
            dataProvider
                .invoke({
                    path: 'apps/' + realmId + '/' + record.id + '/claims',
                    body: JSON.stringify({
                        code,
                        name: 'claimMapping',
                        scopes: [],
                    }),
                    options: {
                        method: 'POST',
                    },
                })
                .then(json => {
                    if (json) {
                        const res = {
                            ...json,
                            context: json.context
                                ? JSON.stringify(json.context, null, 2)
                                : null,
                            result: json.result
                                ? JSON.stringify(json.result, null, 4)
                                : null,
                        };

                        setResult(res);
                        if (json.errors.length > 0) {
                            notify(json.errors[0], { type: 'error' });
                        }
                    } else {
                        notify('ra.notification.bad_item', { type: 'warning' });
                    }
                })
                .catch(error => {
                    const msg = error.message || 'ra.notification.error';
                    notify(msg, { type: 'error' });
                });
        }
    };

    return (
        <>
            <ControlledEditorInput
                source="hookFunctions.claimMapping"
                defaultValue={claimMappingDefaultValue}
                mode="javascript"
                parse={utils.parseBase64}
                format={utils.encodeBase64}
                minLines={25}
            />
            <Button
                disabled={!code}
                onClick={handleTest}
                autoFocus
                variant="contained"
                size="large"
                color="success"
                startIcon={<TestIcon />}
                label={translate('action.test')}
                sx={{ mb: 3 }}
            />

            <Grid container gap={1} mb={2}>
                <Grid item xs={12} md={5}>
                    {result?.context && (
                        <Labeled label="field.context" width="100%">
                            <AceEditorField
                                record={result}
                                source="context"
                                mode="json"
                                theme="solarized_light"
                                width="100%"
                                minLines={25}
                                maxLines={35}
                            />
                        </Labeled>
                    )}
                </Grid>
                <Grid item xs={12} md={5}>
                    {result?.result && (
                        <Labeled label="field.result" width="100%">
                            <AceEditorField
                                record={result}
                                source="result"
                                mode="json"
                                theme="solarized_light"
                                width="100%"
                                minLines={25}
                                maxLines={35}
                            />
                        </Labeled>
                    )}
                </Grid>
            </Grid>
        </>
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
