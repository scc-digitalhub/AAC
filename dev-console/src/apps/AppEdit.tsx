import { Box } from '@mui/material';
import {
    DeleteWithConfirmButton,
    Edit,
    Labeled,
    ReferenceArrayInput,
    ShowButton,
    TabbedForm,
    TextField,
    TextInput,
    TopToolbar,
    useEditContext,
    useGetList,
    useGetOne,
    useRecordContext,
    useRefresh,
    useTranslate,
} from 'react-admin';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { JsonSchemaInput } from '@dslab/ra-jsonschema-input';
import { InspectButton } from '@dslab/ra-inspect-button';
import { SectionTitle } from '../components/sectionTitle';
import { schemaOAuthClient, uiSchemaOAuthClient } from './schemas';
import { Page } from '../components/Page';
import { TabToolbar } from '../components/TabToolbar';
import { AuthoritiesDialogButton } from '../components/AuthoritiesDialog';
import { TestDialogButton } from './TestDialog';
import { RefreshingExportButton } from '../components/RefreshingExportButton';
import { ResourceTitle } from '../components/ResourceTitle';

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
            <TabbedForm.Tab label={translate('page.app.overview.title')}>
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
            <TabbedForm.Tab label={translate('page.app.settings.title')}>
                <SectionTitle
                    text={translate('page.app.settings.header.title')}
                    secondaryText={translate(
                        'page.app.settings.header.subtitle'
                    )}
                />
                <TextInput source="name" fullWidth />
                <TextInput source="description" fullWidth />
            </TabbedForm.Tab>

            <TabbedForm.Tab label="Providers">
                <SectionTitle
                    text={translate('page.app.providers.header.title')}
                    secondaryText={translate(
                        'page.app.providers.header.subtitle'
                    )}
                />
                <ReferenceArrayInput source="providers" reference="idps" />
            </TabbedForm.Tab>

            <TabbedForm.Tab label={translate('page.app.configuration.title')}>
                <SectionTitle
                    text={translate('page.app.configuration.header.title')}
                    secondaryText={translate(
                        'page.app.configuration.header.subtitle'
                    )}
                />
                <JsonSchemaInput
                    source="configuration"
                    schema={schemaOAuthClient}
                    uiSchema={uiSchemaOAuthClient}
                />
            </TabbedForm.Tab>

            <TabbedForm.Tab label="Api access">
                <SectionTitle
                    text={translate('page.app.scopes.header.title')}
                    secondaryText={translate('page.app.scopes.header.subtitle')}
                />
                <ReferenceArrayInput source="scopes" reference="scopes" />
            </TabbedForm.Tab>

            <TabbedForm.Tab label="Roles">
                <SectionTitle
                    text={translate('page.app.roles.header.title')}
                    secondaryText={translate('page.app.roles.header.subtitle')}
                />
                <ReferenceArrayInput source="roles" reference="roles" />
            </TabbedForm.Tab>

            <TabbedForm.Tab label="Groups">
                <SectionTitle
                    text={translate('page.app.groups.header.title')}
                    secondaryText={translate('page.app.groups.header.subtitle')}
                />
                <ReferenceArrayInput source="groups" reference="groups" />
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
