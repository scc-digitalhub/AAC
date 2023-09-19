import * as React from 'react';
import { useState, useEffect } from 'react';
import {
    useRedirect,
    useStore,
    useDataProvider,
    GetListParams,
} from 'react-admin';
import { Chip, Box, MenuItem, Menu } from '@mui/material';

export const RealmListMenu = (props: any) => {
    const dataProvider = useDataProvider();
    const [realms, setRealms] = useState<any[]>([]);
    const [disabled, setDisabled] = useState(false);
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
    const redirect = useRedirect();
    const [realmId, setRealmId] = useStore('realm.selected');

    const handleOpen = (event: React.MouseEvent<HTMLDivElement>) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    const handleAddTag = (realmId: any) => {
        setAnchorEl(null);
        redirect('/dashboard/r/' + realmId);
    };

    // let realmIds: any[] = [
    //     { id: 'system', name: 'system' },
    //     { id: 'nawaz', name: 'nawaz' },
    // ];

    let params: GetListParams = {
        pagination: { page: 0, perPage: 100 },
        sort: { field: 'id', order: 'ASC' },
        filter: undefined,
    };

    useEffect(() => {
        dataProvider.getList('myrealms', params).then((data: any) => {
            if (data && data.data) {
                setRealms(data.data);
            }
        });
    }, []);

    return (
        <>
            <Box>
                <div onClick={handleOpen}>{props.realm}</div>
            </Box>
            <Menu
                open={Boolean(anchorEl)}
                onClose={handleClose}
                anchorEl={anchorEl}
            >
                {realms &&
                    realms.map(realm => (
                        <MenuItem
                            key={realm.id}
                            onClick={() => handleAddTag(realm.id)}
                        >
                            <Chip
                                size="small"
                                variant="outlined"
                                label={realm.name}
                                style={{
                                    backgroundColor: realm.color,
                                    border: 0,
                                }}
                                onClick={() => handleAddTag(realm.id)}
                            />
                        </MenuItem>
                    ))}
            </Menu>
        </>
    );
};
