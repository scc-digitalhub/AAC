import {
    List,
    SearchInput,
    useTranslate,
    ExportButton,
    TopToolbar,
    Datagrid,
    NumberField,
    ArrayField,
    ChipField,
    SingleFieldList,
    TextField,
} from 'react-admin';
import { Box, Divider, Stack } from '@mui/material';
import { YamlExporter } from '../components/YamlExporter';
import { Page } from '../components/Page';
import { PageTitle } from '../components/PageTitle';
import { IdField } from '../components/IdField';
import { NameField } from '../components/NameField';
import { GroupIcon } from '../group/GroupIcon';
import { ShowInDialogButton } from '@dslab/ra-dialog-crud';

export const ApiResourcesList = () => {
    const translate = useTranslate();
    return (
        <Page>
            <PageTitle
                text={translate('page.resources.list.title')}
                secondaryText={translate('page.resources.list.subtitle')}
            />
            <List
                exporter={YamlExporter}
                actions={<ListActions />}
                filters={ListFilters}
                sort={{ field: 'name', order: 'ASC' }}
                component={Box}
                empty={false}
                pagination={false}
            >
                <ResourceListView />
            </List>
        </Page>
    );
};
export const ResourceListView = () => {
    return (
        <Datagrid bulkActionButtons={false} rowClick={false}>
            <NameField
                text="name"
                secondaryText="description"
                source="name"
                icon={false}
            />
            <IdField source="resourceId" label="resource" />
            <ArrayField source="scopes">
                <SingleFieldList linkType={false}>
                    <ChipField source="scope" size="small" />
                </SingleFieldList>
            </ArrayField>
            <ShowInDialogButton variant="contained" maxWidth={'md'} fullWidth>
                <Page>
                    <TextField
                        source="description"
                        variant="h6"
                        sx={{ mb: 1 }}
                    />

                    <ArrayField source="scopes">
                        <SingleFieldList
                            component={Stack}
                            direction={'row'}
                            linkType={false}
                        >
                            <Box sx={{ width: '100%' }}>
                                <NameField
                                    text="name"
                                    secondaryText="scope"
                                    tertiaryText="description"
                                    source="name"
                                    icon={false}
                                />
                                <Divider />
                            </Box>
                        </SingleFieldList>
                    </ArrayField>
                </Page>
            </ShowInDialogButton>
        </Datagrid>
    );
};

const ListActions = () => {
    return (
        <TopToolbar>
            <ExportButton variant="contained" />
        </TopToolbar>
    );
};

const ListFilters = [<SearchInput source="q" alwaysOn />];
