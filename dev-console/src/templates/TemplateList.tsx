import {
    List,
    SearchInput,
    Datagrid,
    TopToolbar,
    useTranslate,
    ExportButton,
    ArrayField,
    ChipField,
    SingleFieldList,
    FunctionField,
} from 'react-admin';
import { Box } from '@mui/material';
import { YamlExporter } from '../components/YamlExporter';

import { isValidElement, ReactElement } from 'react';
import { useRootSelector } from '@dslab/ra-root-selector';
import { TemplateCreateForm } from './TemplateCreate';
import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import { PageTitle } from '../components/PageTitle';
import { ActionsButtons } from '../components/ActionsButtons';
import { IdField } from '../components/IdField';
import { TemplateIcon } from './TemplateIcon';
import { NameField } from '../components/NameField';
import { Page } from '../components/Page';
import { ImportButton } from '../components/ImportButton';

export const TemplateList = () => {
    const translate = useTranslate();
    return (
        <Page>
            <PageTitle
                text={translate('page.templates.list.title')}
                secondaryText={translate('page.templates.list.subtitle')}
            />
            <List
                exporter={YamlExporter}
                actions={<TemplateListActions />}
                filters={TemplateFilters}
                sort={{ field: 'name', order: 'ASC' }}
                component={Box}
                empty={false}
            >
                <TemplateListView />
            </List>
        </Page>
    );
};

export const TemplateListView = (props: {
    actions?: ReactElement | boolean;
}) => {
    const { actions: actionProps = true } = props;

    const actions = !actionProps ? (
        false
    ) : isValidElement(actionProps) ? (
        actionProps
    ) : (
        <ActionsButtons />
    );

    return (
        <Datagrid bulkActionButtons={false} rowClick="edit">
            <NameField
                text="template"
                secondaryText="authority"
                source="template"
                label="field.name.name"
                icon={<TemplateIcon color={'secondary'} />}
            />
            <IdField source="id" label="field.id.name" />
            <ChipField
                source="language"
                label="field.language.name"
                size="small"
            />
            {actions !== false && actions}
        </Datagrid>
    );
};

const TemplateFilters = [<SearchInput source="q" alwaysOn />];

const TemplateListActions = () => {
    const { root: realmId } = useRootSelector();
    const transform = (data: any) => {
        return {
            ...data,
            realm: realmId,
        };
    };

    return (
        <TopToolbar>
            <CreateInDialogButton
                fullWidth
                maxWidth={'sm'}
                variant="contained"
                transform={transform}
            >
                <TemplateCreateForm />
            </CreateInDialogButton>
            <ImportButton variant="contained" idField="templateId" />
            <ExportButton variant="contained" />
        </TopToolbar>
    );
};
