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
} from 'react-admin';
import { useRedirect } from 'react-admin';
import { List as MList, ListItem, ListItemText, Button } from '@mui/material';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';

const RealmListContent = () => {
    const { data: realms, isLoading } = useListContext<any>();
    if (isLoading) {
        return <SimpleListLoading hasLeftAvatarOrIcon hasSecondaryText />;
    }

    return (
        <>
            <BulkActionsToolbar>
                <BulkDeleteButton />
            </BulkActionsToolbar>
            <MList>
                {realms.map(realm => (
                    <RecordContextProvider key={realm.id} value={realm}>
                        <ListItem>
                            <ListItemText
                                primary={`${realm.name}`}
                            ></ListItemText>
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
