import {
    List,
    SearchInput,
    Datagrid,
    TopToolbar,
    ExportButton,
    useTranslate,
    useRecordContext,
} from 'react-admin';
import { YamlExporter } from '../components/YamlExporter';
import { IdField } from '../components/IdField';
import { PageTitle } from '../components/PageTitle';
import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import { AppCreateForm } from './AppCreate';
import { ActionsButtons } from '../components/ActionsButtons';
import { Box, Chip, Stack } from '@mui/material';
import { Page } from '../components/Page';
import { AppIcon } from './AppIcon';
import { NameField } from '../components/NameField';
import { isValidElement, ReactElement } from 'react';
import { useRootSelector } from '@dslab/ra-root-selector';
import { RoleIcon } from '../roles/RoleIcon';
import { GroupIcon } from '../group/GroupIcon';
import DeveloperIcon from '@mui/icons-material/DeveloperMode';
import AdminIcon from '@mui/icons-material/AdminPanelSettings';

export const AppList = () => {
    const translate = useTranslate();
    return (
        <Page>
            <PageTitle
                text={translate('page.app.list.title')}
                secondaryText={translate('page.app.list.subtitle')}
            />
            <List
                exporter={YamlExporter}
                actions={<ListActions />}
                filters={ListFilters}
                sort={{ field: 'name', order: 'DESC' }}
                component={Box}
                empty={false}
            >
                <AppListView />
            </List>
        </Page>
    );
};

export const AppListView = (props: { actions?: ReactElement | boolean }) => {
    const { actions: actionProps = true } = props;

    const actions = !actionProps ? (
        false
    ) : isValidElement(actionProps) ? (
        actionProps
    ) : (
        <ActionsButtons />
    );

    return (
        <Datagrid bulkActionButtons={false} rowClick="show">
            <NameField
                text="name"
                secondaryText="configuration.applicationType"
                tertiaryText="description"
                source="name"
                icon={<AppIcon color={'secondary'} />}
            />
            <IdField source="clientId" label="id" />
            <TagsField />
            {actions !== false && actions}
        </Datagrid>
    );
};

const TagsField = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <Stack direction={'row'} spacing={1}>
            {record.authorities.map(a =>
                a.role === 'ROLE_ADMIN' ? (
                    <Chip label={a.role} color="warning" icon={<AdminIcon />} />
                ) : a.role === 'ROLE_DEVELOPER' ? (
                    <Chip
                        label={a.role}
                        color="warning"
                        icon={<DeveloperIcon />}
                    />
                ) : (
                    <Chip label={a.role} color="warning" />
                )
            )}
            {record.groups.map(g => (
                <Chip label={g.group} icon={<GroupIcon />} color="secondary" />
            ))}
            {record.roles.map(r => (
                <Chip label={r.role} icon={<RoleIcon />} color="secondary" />
            ))}
        </Stack>
    );
};

const ListFilters = [<SearchInput source="q" alwaysOn key={'q'} />];

const ListActions = () => {
    const { root: realmId } = useRootSelector();
    const transform = (data: any) => {
        return {
            ...data,
            realm: realmId,
            type: 'oauth2',
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
                <AppCreateForm />
            </CreateInDialogButton>
            <ExportButton variant="contained" />
        </TopToolbar>
    );
};
