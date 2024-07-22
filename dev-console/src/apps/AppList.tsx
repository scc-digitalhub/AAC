import {
    List,
    SearchInput,
    Datagrid,
    TextField,
    TopToolbar,
    CreateButton,
    ShowButton,
    EditButton,
    ExportButton,
    useTranslate,
} from 'react-admin';
import { YamlExporter } from '../components/YamlExporter';
import { RowButtonGroup } from '../components/RowButtonGroup';
import { IdField } from '../components/IdField';
import { PageTitle } from '../components/pageTitle';
import { DropDownButton } from '../components/DropdownButton';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import { AppCreateForm } from './AppCreate';

export const AppList = () => {
    const translate = useTranslate();
    return (
        <>
            <PageTitle
                text={translate('page.app.list.title')}
                secondaryText={translate('page.app.list.subtitle')}
            />
            <List
                exporter={YamlExporter}
                actions={<AppListActions />}
                filters={RealmFilters}
                sort={{ field: 'name', order: 'DESC' }}
            >
                <Datagrid bulkActionButtons={false}>
                    <TextField source="name" />
                    <IdField source="id" />
                    <RowButtonGroup label="⋮">
                        <DropDownButton>
                            <ShowButton />
                            <EditButton />
                            <DeleteWithDialogButton />
                        </DropDownButton>
                    </RowButtonGroup>
                </Datagrid>
            </List>
        </>
    );
};

const RealmFilters = [<SearchInput source="q" alwaysOn />];
const transform = (data: any) => {
    return {
        ...data,
        configuration: { applicationType: data.type },
        type: 'oauth2',
    };
};
const AppListActions = () => {
    return (
        <TopToolbar>
            <CreateInDialogButton
                fullWidth
                maxWidth={'md'}
                variant="contained"
                transform={transform}
            >
                <AppCreateForm />
            </CreateInDialogButton>
            <ExportButton variant="contained" />
        </TopToolbar>
    );
};
