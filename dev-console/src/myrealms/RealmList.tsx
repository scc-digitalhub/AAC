import {
    List,
    SimpleListLoading,
    useListContext,
    TopToolbar,
    CreateButton,
    Pagination,
    BulkActionsToolbar,
    BulkDeleteButton,
    RecordContextProvider,
    SearchInput,
    EditButton,
    DeleteWithConfirmButton,
} from 'react-admin';
import { useRedirect } from 'react-admin';
import {
    List as MList,
    ListItem,
    ListItemText,
    Button,
    Box,
    Typography,
} from '@mui/material';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import { CustomDeleteButtonDialog } from '../components/CustomDeleteButtonDialog';

const RealmListContent = () => {
    const { data: realms, isLoading } = useListContext<any>();
    if (isLoading) {
        return <SimpleListLoading hasLeftAvatarOrIcon hasSecondaryText />;
    }

    return (
        <>
            <MList>
                {realms.map(realm => (
                    <RecordContextProvider key={realm.id} value={realm}>
                        <ListItem>
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
                                <CustomDeleteButtonDialog
                                    rootId={realm.slug}
                                    property="slug"
                                    title="Realm Deletion"
                                    resourceName="Realm"
                                    registeredResource="myrealms"
                                    redirectUrl={`/myrealms`}
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
        </>
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
            empty={<Empty />}
            actions={<RealmListActions />}
            perPage={25}
            pagination={<Pagination rowsPerPageOptions={[10, 25, 50, 100]} />}
            sort={{ field: 'last_seen', order: 'DESC' }}
            filters={RealmFilters}
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
