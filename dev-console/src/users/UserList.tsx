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
import { Page } from '../components/page';
import { PageTitle } from '../components/pageTitle';
import { YamlExporter } from '../components/YamlExporter';
import { IdField } from '../components/IdField';


const PostBulkActionButtons = () => (
    <>
        <BulkDeleteButton />
    </>
);
export const UserList = () => {
    const { root: realmId } = useRootSelector();
    const record = useRecordContext();
    const translate = useTranslate();
    return (
        <Page>
            <PageTitle
                text={translate('page.user.list.title')}
                secondaryText={translate('page.user.list.subtitle')}
            />
            <List
                empty={false}
                exporter={YamlExporter}
                actions={<UserListActions />}
                filters={UserFilters}
                sort={{ field: 'username', order: 'DESC' }}
                component={Box}
            >
                <Datagrid
                    bulkActionButtons={<PostBulkActionButtons />}
                    rowClick="show"
                >
                    <WrapperField>
                        <Stack>
                            <TextField source="username" />
                            {`${realmId}` !== 'system' && (
                                <span>
                                    <TextField source="email" />
                                    <EmailVerified source="emailVerified" />
                                </span>
                            )}
                        </Stack>
                    </WrapperField>
                    <IdField source="id" />
                    <ArrayField
                        source="authorities"
                    >
                            <SingleFieldList>
                                <ChipField source="authority" size='small'/>
                            </SingleFieldList>
                    </ArrayField>
                    <RowButtonGroup label="⋮">
                        <DropDownButton>
                            <ShowButton />
                            <DeleteWithDialogButton />
                        </DropDownButton>
                    </RowButtonGroup>
                </Datagrid>
            </List>
        </Page>
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
const Empty = () => {
    return (
        <Box textAlign="center" mt={30} ml={70}>
            <Typography variant="h6" paragraph>
                No user, create one
            </Typography>
            <CreateInDialogButton
                fullWidth
                maxWidth={'md'}
                variant="contained"
                transform={transform}
            >
                <UserCreateForm />
            </CreateInDialogButton>
        </Box>
    );
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
