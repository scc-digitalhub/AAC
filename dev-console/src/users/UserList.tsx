import { IconButton, Stack, Box, Typography } from '@mui/material';
import {
    List,
    Datagrid,
    TextField,
    TopToolbar,
    ArrayField,
    ChipField,
    WrapperField,
    useRecordContext,
    SearchInput,
    Button,
    useNotify,
    useUpdate,
    useRefresh,
    ShowButton,
    useTranslate,
    BulkDeleteButton,
    SingleFieldList,
} from 'react-admin';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import BlockIcon from '@mui/icons-material/Block';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import { useRootSelector } from '@dslab/ra-root-selector';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import { UserCreateForm } from './UserCreate';
import { DropDownButton } from '../components/DropdownButton';
import { RowButtonGroup } from '../components/RowButtonGroup';
import { Page } from '../components/Page';
import { PageTitle } from '../components/PageTitle';
import { YamlExporter } from '../components/YamlExporter';
import { IdField } from '../components/IdField';
import { isValidElement, ReactElement } from 'react';
import { ActionsButtons } from '../components/ActionsButtons';
import { NameField } from '../components/NameField';
import { TagsField } from '../components/TagsField';

const PostBulkActionButtons = () => (
    <>
        <BulkDeleteButton />
    </>
);
export const UserList = () => {
    const translate = useTranslate();
    return (
        <Page>
            <PageTitle
                text={translate('page.users.list.title')}
                secondaryText={translate('page.users.list.subtitle')}
            />
            <List
                empty={false}
                exporter={YamlExporter}
                actions={<UserListActions />}
                filters={UserFilters}
                sort={{ field: 'username', order: 'ASC' }}
                component={Box}
            >
                <UserListView />
            </List>
        </Page>
    );
};

export const UserListView = (props: { actions?: ReactElement | boolean }) => {
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
                text="username"
                secondaryText="id"
                tertiaryText="email"
                source="username"
            />
            <IdField source="subjectId" label="id" />
            <TagsField />
            {actions !== false && actions}
        </Datagrid>
    );
};

const UserFilters = [<SearchInput source="q" alwaysOn />];

const UserListActions = () => {
    return (
        <TopToolbar>
            <CreateInDialogButton
                fullWidth
                maxWidth={'md'}
                variant="contained"
                transform={transform}
            >
                <UserCreateForm />
            </CreateInDialogButton>
        </TopToolbar>
    );
};

const EmailVerified = (props: any) => {
    let s = props.source;
    const record = useRecordContext();
    if (!record) return null;
    return (
        <span>
            {record[s] && (
                <IconButton title="Email Verified">
                    <CheckCircleOutlineOutlinedIcon
                        color="success"
                        fontSize="small"
                        sx={{ 'vertical-align': 'bottom' }}
                    />
                </IconButton>
            )}
        </span>
    );
};

const transform = (data: any) => {
    return {
        ...data,
    };
};

const ActiveButton = () => {
    const record = useRecordContext();
    const notify = useNotify();
    const refresh = useRefresh();
    const [inactive] = useUpdate(
        'users',
        {
            id: record.id + '/status',
            data: record,
        },
        {
            onSuccess: () => {
                notify(`user ` + record.id + ` disabled successfully`);
                refresh();
            },
        }
    );
    const [active] = useUpdate(
        'users',
        {
            id: record.id + '/status',
            data: record,
        },
        {
            onSuccess: () => {
                notify(`user ` + record.id + ` enabled successfully`);
                refresh();
            },
        }
    );

    if (!record) return null;
    return (
        <>
            {record.status !== 'inactive' && (
                <Button
                    onClick={() => {
                        record.status = 'inactive';
                        inactive();
                    }}
                    label="Deactivate"
                    startIcon={<BlockIcon />}
                ></Button>
            )}
            {record.status === 'inactive' && (
                <Button
                    onClick={() => {
                        record.status = 'active';
                        active();
                    }}
                    label="Activate"
                    startIcon={<PlayArrowIcon />}
                ></Button>
            )}
        </>
    );
};
