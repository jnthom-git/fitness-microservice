import { Card, CardContent, Grid, Typography } from '@mui/material';
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { getActivities } from '../services/api';

const ActivityList = () => {

    const [activities, setActivities] = useState([]);
    const navigate = useNavigate();

    const fetchActivities = async () => {
        try {
            const response = await getActivities();
            setActivities(response.data);
        } catch (error) {
            console.error(error);
        }
    }

    useEffect(() => {
        fetchActivities();
    }, []);

return (
    <Grid container spacing={2} sx={{ mt: 1, alignItems: 'stretch' }}>
        {activities.map((activity) => (
            <Grid item xs={12} sm={6} md={4} lg={3} key={activity.id} sx={{ display: 'flex' }}>
                <Card
                    sx={{
                        cursor: 'pointer',
                        width: '100%',
                        minHeight: 140,
                        display: 'flex',
                        flexDirection: 'column',
                        justifyContent: 'center',
                        '&:hover': { boxShadow: 4 }
                    }}
                    onClick={() => navigate(`/activities/${activity.id}`)}
                >
                    <CardContent sx={{ py: 1.5 }}>
                        <Typography variant='h6' sx={{ mb: 1 }}>
                            {activity.activityType || activity.type || 'Unknown Activity'}
                        </Typography>
                        <Typography>Duration: {activity.duration}</Typography>
                        <Typography>Calories: {activity.caloriesBurned}</Typography>
                    </CardContent>
                </Card>
            </Grid>
        ))}
    </Grid>
);
};


export default ActivityList;