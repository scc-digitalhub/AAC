import { Box } from '@mui/material';
import {
    AutocompleteArrayInput,
    Edit,
    Labeled,
    NumberField,
    ReferenceArrayInput,
    SaveButton,
    TabbedForm,
    TextField,
    TextInput,
    Toolbar,
    TopToolbar,
    useRecordContext,
    useTranslate,
} from 'react-admin';
import { InspectButton } from '@dslab/ra-inspect-button';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { Page } from '../components/Page';
import { RefreshingExportButton } from '../components/RefreshingExportButton';
import { ResourceTitle } from '../components/ResourceTitle';
import { SectionTitle } from '../components/SectionTitle';
import { DatagridArrayInput } from '@dslab/ra-datagrid-input';
import { IdField } from '../components/IdField';

export const RoleEdit = () => {
    return (
        <Page>
            <Edit
                actions={<ActionsToolbar />}
                mutationMode="optimistic"
                component={Box}
                redirect={'edit'}
            >
                <ResourceTitle />
                <RoleEditForm />
            </Edit>
        </Page>
    );
};

const RoleEditForm = () => {
    const translate = useTranslate();
    const record = useRecordContext();
    if (!record) return null;

    return (
        <TabbedForm toolbar={<TabToolbar />} syncWithLocation={false}>
            <TabbedForm.Tab label="tab.overview">
                <Labeled>
                    <TextField source="id" label="field.id.name" />
                </Labeled>
                <Labeled>
                    <TextField source="role" label="field.role.name" />
                </Labeled>
                <Labeled>
                    <TextField
                        source="authority"
                        label="field.authority.name"
                    />
                </Labeled>
                <Labeled>
                    <NumberField source="size" label="field.subejcts.name" />
                </Labeled>
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.settings">
                <SectionTitle
                    text={translate('page.role.settings.header.title')}
                    secondaryText={translate(
                        'page.group.settings.header.subtitle'
                    )}
                />
                <TextInput
                    source="role"
                    label="field.role.name"
                    helperText="field.role.helperText"
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
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.permissions">
                <SectionTitle
                    text={translate('page.roles.permissions.header.title')}
                    secondaryText={translate(
                        'page.roles.permissions.header.subtitle'
                    )}
                />
                <ReferenceArrayInput source="permissions" reference="scopes">
                    <AutocompleteArrayInput
                        label="field.permissions.name"
                        helperText="field.permissions.helperText"
                    />
                </ReferenceArrayInput>
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.subjects">
                <SectionTitle
                    text={translate('page.roles.subjects.header.title')}
                    secondaryText={translate(
                        'page.group.subjects.members.subtitle'
                    )}
                />
                <ReferenceArrayInput source="subjects" reference="subjects">
                    <DatagridArrayInput
                        dialogFilters={[<TextInput label="query" source="q" />]}
                        dialogFilterDefaultValues={{ q: '' }}
                        label="field.subjects.name"
                        helperText="field.subjects.helperText"
                    >
                        <TextField
                            source="name"
                            label="field.permissions.name"
                        />
                        <TextField source="type" label="field.type.name" />
                        <IdField source="id" label="field.id.name" />
                    </DatagridArrayInput>
                </ReferenceArrayInput>
            </TabbedForm.Tab>
        </TabbedForm>
    );
};

const ActionsToolbar = () => {
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

const TabToolbar = () => (
    <Toolbar>
        <SaveButton />
    </Toolbar>
);
