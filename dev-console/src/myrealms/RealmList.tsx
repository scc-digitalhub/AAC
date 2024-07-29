import {
    List,
    SimpleListLoading,
    useListContext,
    TopToolbar,
    CreateButton,
    Pagination,
    RecordContextProvider,
    SearchInput,
    EditButton,
    useRedirect,
} from 'react-admin';
import {
    List as MList,
    ListItem,
    ListItemText,
    Button,
    Box,
    Typography,
    ListItemIcon,
    IconButton,
} from '@mui/material';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import { DeleteButtonDialog } from '../components/DeleteButtonDialog';
import GroupsIcon from '@mui/icons-material/Groups';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';

const RealmListContent = () => {
    const { data: realms, isLoading } = useListContext<any>();
    if (isLoading) {
        return <SimpleListLoading hasLeftAvatarOrIcon hasSecondaryText />;
    }

    return (
        <MList>
            {realms.map(realm => (
                <RecordContextProvider key={realm.id} value={realm}>
                    <ListItem>
                        {realm.public && (
                            <IconButton title="Public">
                                <GroupsIcon />
                            </IconButton>
                        )}
                        {!realm.public && (
                            <IconButton title="Private">
                                <VisibilityOffIcon />
                            </IconButton>
                        )}
                        &nbsp; &nbsp;
                        <ListItemText
                            primary={`${realm.slug}`}
                            secondary={`${realm.name}`}
                        ></ListItemText>
                        {realm.slug !== 'system' && (
                            <EditButton
                                sx={{ fontSize: '14.4px' }}
                                to={`/myrealms/${realm.slug}/edit`}
                            />
                        )}
                        {realm.slug !== 'system' && (
                            <DeleteButtonDialog
                                confirmTitle="Realm Deletion"
                                redirect={`/myrealms`}
                            />
                        )}
                        <ManageButton
                            selectedId={realm.id}
                            selectedName={realm.name}
                        ></ManageButton>
                    </ListItem>
                </RecordContextProvider>
            ))}
        </MList>
    );
};

const RealmListActions = () => (
    <TopToolbar>
        <CreateButton
            variant="contained"
            label="New Realm"
            sx={{ marginLeft: 2 }}
        />
    </TopToolbar>
);

export const RealmList = () => {
    return (
        <List
            actions={<RealmListActions />}
            perPage={25}
            pagination={<Pagination rowsPerPageOptions={[10, 25, 50, 100]} />}
            sort={{ field: 'last_seen', order: 'DESC' }}
            filters={RealmFilters}
            empty={false}
        >
            <RealmListContent />
        </List>
    );
};

const Empty = () => {
    return (
        <Box textAlign="center" mt={30} ml={70}>
            <Typography variant="h6" paragraph>
                No realm available, create one
            </Typography>
            <CreateButton
                variant="contained"
                label="New Realm"
                sx={{ marginLeft: 2 }}
            />
        </Box>
    );
};

const ManageButton = (params: any) => {
    const redirect = useRedirect();

    return (
        <>
            <Button
                endIcon={<CheckCircleOutlinedIcon />}
                onClick={() => {
                    redirect('/dashboard/r/' + params.selectedId);
                }}
            >
                Manage
            </Button>
        </>
    );
};

const RealmFilters = [
    <SearchInput placeholder="Search by realm name" source="q" alwaysOn />,
];
