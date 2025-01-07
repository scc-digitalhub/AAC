import { Box } from '@mui/material';

import { isValidElement, ReactElement } from 'react';
import {
    Datagrid,
    List,
    SearchInput,
    TopToolbar,
    useRecordContext,
    ExportButton,
    useTranslate,
    BooleanField,
    FunctionField,
} from 'react-admin';
import { YamlExporter } from '../components/YamlExporter';
import { useRootSelector } from '@dslab/ra-root-selector';
import { ApCreateForm } from './ApCreate';
import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import { ActionsButtons } from '../components/ActionsButtons';
import { IdField } from '../components/IdField';
import { PageTitle } from '../components/PageTitle';
import { Page } from '../components/Page';
import { NameField } from '../components/NameField';
import { ApIcon } from './ApIcon';
import { getApIcon } from './utils';
import EnabledIcon from '@mui/icons-material/CheckCircleOutlined';
import DisabledIcon from '@mui/icons-material/CancelOutlined';
import RegisteredIcon from '@mui/icons-material/VerifiedUser';
import WarningIcon from '@mui/icons-material/WarningOutlined';
import { ImportButton } from '../components/ImportButton';

export const ApList = () => {
    const translate = useTranslate();
    return (
        <Page>
            <PageTitle
                text={translate('page.aps.list.title')}
                secondaryText={translate('page.aps.list.subtitle')}
            />
            <List
                exporter={YamlExporter}
                actions={<ListActions />}
                filters={ListFilters}
                sort={{ field: 'name', order: 'ASC' }}
                component={Box}
                empty={false}
            >
                <ApListView />
            </List>
        </Page>
    );
};

export const ApListView = (props: { actions?: ReactElement | boolean }) => {
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
            <ApNameField source="name" label="field.name.name" />
            <IdField source="provider" label="field.id.name" />
            <BooleanField
                source="enabled"
                label="field.enabled.name"
                sortable={false}
                TrueIcon={EnabledIcon}
                FalseIcon={DisabledIcon}
            />

            <FunctionField
                source="registered"
                label="field.registered.name"
                sortable={false}
                render={record => {
                    if (!record.enabled) return <></>;
                    return (
                        <BooleanField
                            source="registered"
                            sortable={false}
                            TrueIcon={RegisteredIcon}
                            FalseIcon={WarningIcon}
                            color={record.registered ? 'primary' : 'error'}
                        />
                    );
                }}
            />
            {actions !== false && actions}
        </Datagrid>
    );
};

export const ApNameField = (props: { source: string; label?: string }) => {
    const { label } = props;
    const record = useRecordContext();
    const color = record?.registered
        ? 'primary'
        : record?.enabled
        ? 'warning'
        : 'info';

    const iconColor = record?.registered
        ? 'primary'
        : record?.enabled
        ? 'error'
        : 'disabled';

    const icon = record ? (
        getApIcon(record.authority, {
            color: iconColor,
        })
    ) : (
        <ApIcon />
    );

    return (
        <NameField
            text="name"
            secondaryText="authority"
            source="name"
            label={label}
            icon={icon}
            color={color}
        />
    );
};

const ListFilters = [<SearchInput source="q" alwaysOn />];

const ListActions = () => {
    const { root: realmId } = useRootSelector();
    const transform = (data: any) => {
        return {
            ...data,
            type: 'attributes',
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
                <ApCreateForm />
            </CreateInDialogButton>
            <ImportButton variant="contained" idField="provider" />
            <ExportButton variant="contained" />
        </TopToolbar>
    );
};
