import { Box } from '@mui/material';
import {
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

export const GroupEdit = () => {
    //fetch related to resolve relations
    const { data: roles } = useGetList('roles', {
        pagination: { page: 1, perPage: 100 },
        sort: { field: 'name', order: 'ASC' },
    });

    //inflate back flattened fields
    const transform = data => ({
        ...data,
        roles: roles?.filter(r => data.roles.includes(r.id)),
    });

    return (
        <Page>
            <Edit
                actions={<GroupToolBarActions />}
                mutationMode="optimistic"
                component={Box}
                redirect={'edit'}
                transform={transform}
                queryOptions={{ meta: { flatten: ['roles'] } }}
                mutationOptions={{ meta: { flatten: ['roles'] } }}
            >
                <ResourceTitle />
                <GroupEditForm />
            </Edit>
        </Page>
    );
};

const GroupEditForm = () => {
    const translate = useTranslate();
    const record = useRecordContext();
    if (!record) return null;

    return (
        <TabbedForm toolbar={<TabToolbar />} syncWithLocation={false}>
            <TabbedForm.Tab label="tab.overview">
                <Labeled>
                    <TextField source="id" />
                </Labeled>
                <Labeled>
                    <TextField source="name" />
                </Labeled>
                <Labeled>
                    <TextField source="group" />
                </Labeled>
                <Labeled>
                    <NumberField source="size" label="members" />
                </Labeled>
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.settings">
                <SectionTitle
                    text={translate('page.group.settings.header.title')}
                    secondaryText={translate(
                        'page.group.settings.header.subtitle'
                    )}
                />
                <TextInput source="group" fullWidth readOnly />
                <TextInput source="name" fullWidth />
                <TextInput source="description" multiline fullWidth />
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.roles">
                <SectionTitle
                    text={translate('page.group.roles.header.title')}
                    secondaryText={translate(
                        'page.group.roles.header.subtitle'
                    )}
                />
                <ReferenceArrayInput source="roles" reference="roles" />
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.members">
                <SectionTitle
                    text={translate('page.group.members.header.title')}
                    secondaryText={translate(
                        'page.group.roles.members.subtitle'
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

const GroupToolBarActions = () => {
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
